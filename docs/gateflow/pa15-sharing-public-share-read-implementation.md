# PA15 Public Share Read — Blocked Implementation Intake

- Task: `pa15-public-share-read-implementation-gpt-20261106`.
- Work unit: `PA15-sharing-public-share-read`.
- Gate: `implementation`; status: **BLOCKED before production/test implementation**.
- Date: 2026-11-06, Asia/Shanghai.
- Accepted plan: `docs/gateflow/pa15-sharing-public-share-read-plan.md`, 372 lines, SHA256 `07605141492b281337fc37046c01cd5995c071867970d5fa7bb17ec7b38d26ae`.
- Live HEAD observed: `ffadc2eceebd5cf80f68f5e864750ac7727510e2`.
- This is a stop-condition report, not a completed candidate, review, gate advancement or production-readiness claim.

## 1. Reproduced Contract Blocker

The accepted plan §4, item 3, explicitly requires an encoded slash to receive a sanitized **404 NOT_FOUND**, before authentication, service lookup or database access. It also requires the established REQUEST-only filter registration to stay unchanged. Section 3 permits only the public-route/optional-authentication change in `CustomerBoundaryFilter` and expressly excludes web configuration, error-controller and settings changes. Its stop rule requires a bounded plan correction when the necessary implementation is outside the authorized design.

The unchanged packaged application instead rejects an encoded slash **before the servlet filter**:

| Synthetic GET presentation | Actual status | Actual content type | Cache-Control | Required PA15 result |
| --- | --- | --- | --- | --- |
| `/api/v1/public/shares/a%2Fb` | 400 | `text/html;charset=utf-8` | Absent | Closed 404 NOT_FOUND Problem, no-store |
| `/api/v1/public/shares/a%2fb` | 400 | `text/html;charset=utf-8` | Absent | Closed 404 NOT_FOUND Problem, no-store |
| `/api/v1/public/shares/a%252Fb` | 404 | `application/problem+json` | no-store | Closed 404 NOT_FOUND Problem |
| `/api/v1/public/shares/a%20b` | 404 | `application/problem+json` | no-store | Closed 404 NOT_FOUND Problem |
| `/api/v1/public/shares/a;b` | 404 | `application/problem+json` | no-store | Closed 404 NOT_FOUND Problem |

The canonical synthetic public path also returns a closed 404 on this baseline, because the public endpoint has not been implemented; it is a negative control, not a successful public read. The existing customer path with an encoded slash likewise receives HTML 400.

The first diagnostic made seven HTTP requests. A separate confirmation made 24 requests: two context paths (root and `/pa15`), three header combinations (no headers; malformed supplied Authorization; that Authorization plus `Accept: text/html`), and four token-segment presentations (`synthetic`, `a%2Fb`, `a%2fb`, `a%252Fb`). All 12 encoded-slash requests returned HTML 400 with no Cache-Control. All 12 control requests returned closed 404 NOT_FOUND Problems with no-store. The context prefix and optional credentials therefore do not account for the discrepancy. All three diagnostic web JVMs were terminated and reaped; each exited 143 after termination, with graceful-shutdown messages.

This is a connector-layer limitation, not an unimplemented public matcher:

- The packaged dependency is Apache Tomcat **11.0.27**, confirmed by the JAR inventory and startup log.
- Local `javap -c -p` inspection of that exact `tomcat-embed-core` JAR shows `Connector` initializes `encodedSolidusHandling` to `EncodedSolidusHandling.REJECT`.
- `CoyoteAdapter.postParseRequest` calls `UDecoder.convert` with the connector's encoded-solidus setting and catches the decoding IOException by sending status 400. This occurs during container URI processing, before servlet filtering.
- `CustomerBoundaryFilter.doFilter` sets the Problem writer's headers unconditionally before routing (`CustomerBoundaryFilter.java:48`). The encoded-slash responses have neither those headers nor the closed Problem body. Ordinary rejected paths have both.
- `CustomerWebConfiguration.java:22` registers only REQUEST dispatches at highest precedence. A servlet-filter whitelist change or MVC controller cannot rewrite a response generated before that filter is entered. The existing error controller did not convert these responses during the packaged probes.

No connector customization, extra filter, container valve, system-property workaround, dependency change, deployment change or permissive test substitution was attempted. Hiding a container customizer in an allowlisted controller/filter/persistence file would violate that file's frozen responsibility and the accepted design, even if its filename were allowlisted.

**Required owner decision:** Controller/Backend must provide a bounded plan correction that either authorizes appropriate pre-servlet handling/configuration and its regression proof, or explicitly distinguishes container-rejected URI presentations from servlet-admitted route rejections. This report does not select or implement either alternative. No existing or planned assertion is weakened. In particular, the supplied prompt's prepared-statement clarification is accepted and is not the blocker.

## 2. Exact Write Boundary

The only task-authored repository file is:

`docs/gateflow/pa15-sharing-public-share-read-implementation.md`

All 23 accepted candidate paths below remain untouched. Before implementation, nine existed and fourteen were absent; those counts remain unchanged.

1. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java`
2. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java`
3. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/PublicSharesController.java`
4. `backend/src/main/java/com/onecall/core/customer_iam/package-info.java`
5. `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/package-info.java`
6. `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/OpaqueCapabilityToken.java`
7. `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryException.java`
8. `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareView.java`
9. `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryService.java`
10. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/PublicShareQueryRepository.java`
11. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqPublicShareQueryRepository.java`
12. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/PublicSharePersistenceConfiguration.java`
13. `backend/src/test/java/com/onecall/archtests/PublicShareWebTests.java`
14. `backend/src/test/java/com/onecall/archtests/sharing/PublicShareQueryServiceTests.java`
15. `backend/src/test/java/com/onecall/archtests/PublicSharePersistenceConfigurationTests.java`
16. `backend/src/test/java/com/onecall/archtests/container/PublicSharesFixture.java`
17. `backend/src/test/java/com/onecall/archtests/container/PublicSharesIT.java`
18. `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`
19. `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`
20. `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`
21. `backend/pom.xml`
22. `.github/workflows/ci.yml`
23. `backend/README.md`

Generated compilation, XML and baseline diagnostic boot-log outputs are confined to ignored `backend/target/`; they are not source changes or a completed implementation candidate. The accepted plan, fixes, Control, reviews, V001–V010, OpenAPI/generated API, deployment files and existing source/tests remain unchanged. The pre-existing untracked private `.codex` entry was observed in Git status only; its contents and old worktrees were not inspected or changed.

## 3. One-Slice Mapping And Production Decisions

The authorized slice remains exactly `GET /api/v1/public/shares/{capabilityToken}`. No independent lower-layer implementation was started after discovering the HTTP acceptance blocker.

| Slice segment | Actual implementation status |
| --- | --- |
| Canonical route and optional strict customer authentication | Not implemented; encoded-slash pre-filter contradiction reproduced |
| Token shape, exact UTF-8 SHA-256 lookup and unresolved-token concealment | Not implemented |
| PostgreSQL-enforced command-pool read-only transaction and role assertions | Not implemented |
| REVOKED/EXPIRED/time precedence and lazy current grant-tenant membership | Not implemented |
| Immutable evidence-owner comparison and closed omitted-field projection | Not implemented |
| No-write observations, regressions, exact architecture partition and CI inventories | Not implemented or certified |

No production transaction/role/digest/member/projection decision is claimed as implemented. The frozen choices remain binding for a future authorized retry: String or Jackson JsonNode snapshot carrier; service-only DSLContext exception; no-DSL repository interface; MANDATORY evidence reader; indexed PREPARED_STATEMENT binding with SQL/execute/bind logging disabled; current ACTIVE account plus ACTIVE grant-tenant membership independent of selected workspace; and absent rather than null optional content. No dependency, token format/generator, verifier, KDF, TTL, rate limit or deployment value was invented.

## 4. Environment And Baseline

Before source editing, captured `java -version`, the wrapper invocation, `docker info`, current test inventory and Git status/path baseline.

- The initial shell resolved Java to the Windows executable through `/mnt/d/tools/jdk21/bin/java`: Temurin **21.0.12.1**. Linux execution was recovered using the already-installed `/opt/jdk21`, without changing files or machine configuration.
- Actual Maven/test/JAR runtime: Temurin **21.0.13+9**, OpenJDK 64-bit, Linux amd64.
- Actual Maven: **3.9.9**, wrapper distribution `/root/.m2/wrapper/dists/apache-maven-3.9.9/3477a4f1`.
- Docker client/server: **29.1.3 / 29.1.3**; default context; server reports Ubuntu 22.04.5 LTS, WSL2 kernel `6.18.33.2-microsoft-standard-WSL2`, x86_64. `docker info` succeeded. No task PostgreSQL/Testcontainers instance was started.
- PostgreSQL 16 runtime version: **not measured**. Docker availability is not PostgreSQL evidence.
- Bare Git initially failed because `.git` contains the Windows-form pointer `D:/codex-call/.git/worktrees/codex-call15`. Process-local `GIT_DIR`, `GIT_WORK_TREE` and `GIT_OPTIONAL_LOCKS=0` resolved the live Git directory to exactly `/mnt/d/codex-call/.git/worktrees/codex-call15`; `.git` was not rewritten.
- Initial tracked diff was empty. Initial status contained only the pre-existing untracked `.codex` entry. The implementation artifact was confirmed absent before creation.
- `./mvnw -v` at repository root failed because the wrapper is under `backend/`. Direct `backend/./mvnw -v` then failed with `/bin/sh^M: bad interpreter` because its existing checkout has CRLF. The wrapper was executed from an in-memory LF-normalized script with its original `$0`; no wrapper/POM/property edit was made.
- The accepted plan was read through EOF and its exact hash/line count verified. Intake examined active Control/Goal Confirmation, fix/review material, the frozen public OpenAPI schemas and relevant filter, error, authentication, context, persistence, evidence, architecture-test and CI sources. The blocker was established before completing the remaining full prerequisite schema/code/test intake; this report does not claim an exhaustive read of every requested authority.

## 5. Commands And Results Actually Obtained

Linux environment and wrapper invocation used for the successful commands:

```sh
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
cd backend
sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -v
sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -B -ntp -Dtest=CustomerApiBoundaryTests,SharingQueryBoundaryTests,ModuleBoundaryTests test
sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -B -ntp -DskipTests package
```

| Executed validation | Actual result |
| --- | --- |
| Wrapper version with recovered Linux JAVA_HOME | Maven 3.9.9; Java 21.0.13 |
| Existing three-class architecture baseline | BUILD SUCCESS; 3 XML suites, 12 methods, 0 failures/errors/skips |
| Baseline packaging with `-DskipTests` | BUILD SUCCESS; tests intentionally not executed by this packaging command |
| Baseline web-only packaged-JAR diagnostics | 7 initial plus 24 confirmation HTTP requests; encoded-slash contract mismatch reproduced, not an acceptance pass |
| Exact local Tomcat bytecode inspection | REJECT default and decoder-to-400 handling confirmed |
| Current source versus CI literal method inventories | Exact class/method-set equality: 16/79 Surefire, 6/118 Failsafe, 197 combined |
| Available architecture XML versus its source/CI subsets | Exact method identities: CustomerApiBoundaryTests 3, SharingQueryBoundaryTests 1, ModuleBoundaryTests 8 |

The diagnostic launch command was `java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar --spring.profiles.active=web --server.port=0`, with `--server.servlet.context-path=/pa15` for the non-root confirmation. Inline Python discovered each ephemeral port from the startup log, imposed a 60-second startup bound and 10-second HTTP timeout, issued the exact matrices in §1, and terminated/reaped every JVM with 15-second termination and five-second kill bounds. Confirmation assertions checked the actual HTML 400 response and absence of Cache-Control, versus exact five-key NOT_FOUND Problems and no-store for controls. These assertions confirm the blocker; they deliberately do not reinterpret HTML 400 as acceptable PA15 behavior. Diagnostic boot logs contain no HikariPool startup.

Retained local generated evidence:

- `backend/target/surefire-reports/TEST-com.onecall.archtests.architecture.CustomerApiBoundaryTests.xml`
- `backend/target/surefire-reports/TEST-com.onecall.archtests.architecture.SharingQueryBoundaryTests.xml`
- `backend/target/surefire-reports/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml`
- `backend/target/pa15-baseline-route-probe.log`
- `backend/target/pa15-baseline-root-confirmation.log`
- `backend/target/pa15-baseline-context-confirmation.log`

## 6. Mandatory Evidence Not Obtained

- No new test class/fixture or method was added. The planned 6+4+2 new Surefire and eight PublicSharesIT methods remain absent. Architecture methods were not changed.
- The required focused **six-class** Surefire command was not run; only its three existing architecture classes were run as a baseline.
- Targeted `-Pcontainer-smoke -Dit.test=PublicSharesIT verify` was not run. The class does not exist on this stopped candidate.
- Fresh full clean `clean -Pcontainer-smoke verify` was not run. There is no 19-suite/91-method Surefire or seven-suite/126-method Failsafe XML result, no 217-test combined result and no full source=CI=XML acceptance equality.
- Current source=CI remains 79+118=197; actual available XML is only 3/12 Surefire and no Failsafe suites. Partial baseline XML is not a substitute for a full acceptance run.
- No PostgreSQL version, transaction_read_only/current_user/backend-PID, physical connection reuse, intentional SQLSTATE 25006, role restore, pool reset or before/after persistent no-write snapshot evidence was obtained. No database-read-only or no-write guarantee is certified.
- The complete updated default/web JAR smoke suite, successful public-read probes, production-JAR dependency/firewall check and all 82 adversarial probes were not run. Baseline packaging and route diagnostics do not satisfy those requirements.
- POM includes, CI inventories/smokes and README counts were deliberately not changed, because there is no implemented slice or new test inventory to synchronize.
- Create/revoke/rotate/list/`me` behavior remains source-unchanged, but their full runtime regressions were not run in this task.

## 7. Scope Audit And Residual Owners

Read-only Git checks use the confirmed live directory, not another worktree:

```sh
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15 GIT_WORK_TREE="$PWD" GIT_OPTIONAL_LOCKS=0 git rev-parse --git-dir
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15 GIT_WORK_TREE="$PWD" GIT_OPTIONAL_LOCKS=0 git diff --check
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15 GIT_WORK_TREE="$PWD" GIT_OPTIONAL_LOCKS=0 git diff --name-only
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15 GIT_WORK_TREE="$PWD" GIT_OPTIONAL_LOCKS=0 git status --porcelain=v1 --untracked-files=normal
```

The final audit must include this new untracked artifact explicitly, not infer it from an empty tracked diff. No protected/private source path is authorized by this report. Git's LF-to-CRLF checkout warnings are not treated as file edits; the accepted plan hash is checked independently.

| Unresolved item | Owner / required destination |
| --- | --- |
| Encoded-slash rejection happens before the only authorized filter | Controller/Backend: bounded plan correction before implementation resumes |
| All public-read behavior and the full 217-test/JAR/firewall/no-write proof | Implementation author after corrected authorization |
| Production session verifier remains absent | Security/Identity; preserve fail-closed behavior |
| Dual-role command-pool provisioning, capacity and dedicated least-privilege read principal | Backend/Database/Platform/Security; no PA15 schema/deployment workaround |
| READ COMMITTED observation is not an admission/media authorization lease | Sharing/Product/Security; later admission/media owners recheck current state |

No Git staging, commit, push, PR, new worktree/branch, merge, approval, ready action, reviewer request, external comment, issue mutation or later-gate action occurred. No external review or remote CI was initiated. Status remains blocked at implementation; no production-readiness claim is made.
