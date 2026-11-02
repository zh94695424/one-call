# PA11 S1 implementation completion report

## 1. Task, authority and working-tree identity

- Reporting task: `pa11-s1-report-gpt-20261102-01`; work unit: `PA11-customer-auth-api-foundation`; slice: `S1`; report date: 2026-11-02.
- Role: implementation reporter. The implementation, tests, CI and README already existed at reporting entry. This task adds only this report; it does not implement another change or enter a review gate. The requested `gpt-6.1-sol` model is not available in this reporting session; no model-switch claim is made.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa11-customer-auth-api-foundation/codex-call`.
- Start HEAD and final HEAD: `d4b6ff68246027c61591505828fe69dafefe4ea9`. HEAD tree: `bbca4fb1fc52b4a22479627630b03a8b45fa5635`. No new commit/tree object represents the uncommitted implementation.
- Git inspection uses process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call9` and `GIT_WORK_TREE=$PWD`; the checkout's Windows-form gitdir otherwise fails under WSL. No repository configuration is changed.
- Accepted plan SHA256: `70fce4d677e9c77bd9389d3ec3db9ecbdeacd127a91930c107658db118bc5f5e`; fix-3 SHA256: `fa80ee71318d70695a6112ca8cbf95672f950011b4d31be6ddd83ec6a158fb41`. Current control-file SHA256: `76da0d290dfff047d548f531e49dcd00ec54a0ae11e45ed5ccfb9052457b6dfc`. These are recomputed working-tree bytes, not substituted historical controller hashes.
- Final implementation identity: complete 33-file product/test/CI/README diff SHA256 `4676ea1a98c608765d81e22a1e73694461e1520425f57f53687956b1d82dbbe4`; product JAR SHA256 `dbc43d4c8ccf3f05567410b0f1da67e923caf9864c5600b7972feceb8e4d7f80`.
- Reporting entry has six tracked modifications and 27 untracked implementation files, exactly the 33 files below. Reporting completion adds this one untracked report, for 34 changed/new files in total. The index is unchanged and contains no staged changes. The 33-file diff and hash manifest intentionally exclude this report, avoiding a circular report hash.

## 2. Goal, non-goals and allowed-file boundary

The implemented goal is an explicitly opt-in customer servlet boundary for only `GET /api/v1/customer/me`, using a provider-neutral opaque-session verifier port and current database authorization facts. The response is the frozen account/workspace/channel projection. Default startup remains non-web and database-free. Missing authentication fails closed before persistence initialization; no real production verifier is supplied.

| Allowed boundary | Actual implementation and evidence |
| --- | --- |
| Nineteen new internal customer types under `backend/src/main/java/com/onecall/core/customer_iam/internal/` | Four application, four persistence, five security and six web classes; detailed below. No module root API, named interface, annotation or dependency-map edit. |
| New `backend/src/main/resources/application-web.yml` | Servlet opt-in, empty-default customer credential placeholders and restricted logging only. Existing `application.yml` and `application-persistence.yml` unchanged. |
| One PA10 configuration edit | Explicit qualifiers only in `SharingPersistenceConfiguration`; no PA10 service/repository behavior or SQL edit. |
| Seven new test/support files plus two existing-test extensions | Exactly the seven additions and two modifications listed below; no unrelated existing test edits. |
| `backend/pom.xml` | One managed compile MVC dependency and one additional explicit Failsafe include; existing pins/scopes/security bans retained. |
| `.github/workflows/ci.yml` | Exact test inventories, positive-first two-inventory firewall, bounded default/web JAR smokes; Java job remains capped at 30 minutes. |
| `backend/README.md` | Current PA11 capabilities, profiles/credentials/refusal semantics, dependency manifests, firewall and 49/84/133 inventory; PA0–PA10 results remain historical. |
| This reporting task | Only `docs/gateflow/pa11-customer-auth-api-foundation-s1-implementation.md` is created. Plan, controller, historical implementation/reviews and all implementation inputs remain untouched. |

Non-goals remain login/sign-up/logout/refresh, provider selection/adapters, WeChat/OTP, identity merge, issuer/session-store/TTL policy, Redis, cookies, employee/IVR/public-share/callback authentication, any other customer/business API, mutation, frontend, provisioning, schema/index/ACL/migration/OpenAPI/security-contract changes, or production readiness. The cross-schema SELECT is the existing T-S1-04 grant, not a Java module dependency or a new generic cross-owner query facility. Workspace visibility is advisory, not an authorization lease.

### Exact 33-file implementation changed list

This is the complete ordered content of `/tmp/pa11-web-probe/implementation-changed-files.txt`, independently reconciled with tracked changes plus all nonignored untracked files before this report was added:

```text
.github/workflows/ci.yml
backend/README.md
backend/pom.xml
backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerContext.java
backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerContextException.java
backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerContextService.java
backend/src/main/java/com/onecall/core/customer_iam/internal/application/TrustedCustomerCaller.java
backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerContextRepository.java
backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java
backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceProperties.java
backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/JooqCustomerContextRepository.java
backend/src/main/java/com/onecall/core/customer_iam/internal/security/CustomerBearerAuthenticator.java
backend/src/main/java/com/onecall/core/customer_iam/internal/security/CustomerSessionVerificationException.java
backend/src/main/java/com/onecall/core/customer_iam/internal/security/CustomerSessionVerifier.java
backend/src/main/java/com/onecall/core/customer_iam/internal/security/OpaqueCustomerBearer.java
backend/src/main/java/com/onecall/core/customer_iam/internal/security/VerifiedCustomerSession.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerApiErrorController.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerMeController.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerRequestPrincipal.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingPersistenceConfiguration.java
backend/src/main/resources/application-web.yml
backend/src/test/java/com/onecall/archtests/CustomerMeWebTests.java
backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java
backend/src/test/java/com/onecall/archtests/OneCallApplicationTests.java
backend/src/test/java/com/onecall/archtests/architecture/BootstrapDependencyBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java
backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java
backend/src/test/java/com/onecall/archtests/container/CustomerMeIT.java
backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerContextServiceTests.java
backend/src/test/java/com/onecall/core/customer_iam/internal/security/CustomerSessionBoundaryTests.java
```

## 3. Exact implementation summary

All class paths in the following table are relative to `backend/src/main/java/com/onecall/core/customer_iam/internal/`.

| Production class (19 total) | Implemented responsibility |
| --- | --- |
| `application/CustomerContext.java` | Immutable record with defensive workspace copy; account has `accountId,displayName,status`; PERSONAL has only `workspaceType,status`; organization workspace adds `organizationId,organizationDisplayName,role`; nullable workspace members are omitted locally, not by a global mapper policy. Response includes attested `MINIAPP`/`WEB` channel. |
| `application/CustomerContextException.java` | Stackless, cause-free fixed codes `UNAUTHENTICATED,FORBIDDEN,VALIDATION_FAILED,RATE_LIMITED,SERVICE_UNAVAILABLE,INTERNAL`; never retains raw technical text. |
| `application/CustomerContextService.java` | Accepts only `TrustedCustomerCaller`; starts the qualified read-only transaction, executes fixed `SET LOCAL ROLE customer_iam_role`, asserts `current_user`, fetches and defensively validates the complete projection. Missing/disabled account becomes 401; database/transaction/bean failures become 503; programming/projection failures become 500. Cause traversal is cycle-safe and no partial context is returned. |
| `application/TrustedCustomerCaller.java` | Typed account UUID and attested channel only; no request selectors, bearer or organization/role override. |
| `persistence/CustomerContextRepository.java` | Single internal `Optional<CustomerContext> findActiveContext(TrustedCustomerCaller)` port. |
| `persistence/CustomerPersistenceConfiguration.java` | Web-profile, lazy, explicitly qualified pool/transaction-manager/template/DSL/repository/service graph; credentials validated before Hikari construction; close-managed pool; READ_COMMITTED, REQUIRES_NEW, read-only transaction; PostgreSQL jOOQ prepared/indexed statements through a transaction-aware datasource proxy with execution logging disabled. Unspecified Hikari settings remain vendor defaults. |
| `persistence/CustomerPersistenceProperties.java` | Binds `onecall.customer.persistence` URL/username/password; explicit nonblank validation with a fixed error message, no fallback credentials. |
| `persistence/JooqCustomerContextRepository.java` | One prepared SELECT: ACTIVE account by attested UUID, LEFT JOIN ACTIVE membership under ACTIVE organization; bindings exactly `ACTIVE,ACTIVE,accountId,ACTIVE`; PERSONAL synthesized first, organizations sorted by PostgreSQL UUID. No locks, identity/provider lookup or writes. |
| `security/CustomerBearerAuthenticator.java` | Requires exactly one Authorization header matching opaque bearer syntax; selects exactly one verifier, supplies fixed audience `onecall:customer-session:v1`, rejects wrong audience, malformed results and absent/ambiguous/unavailable adapters. Missing/malformed bearer is 401; adapter/technical result failure is 503. No JWT decoding. |
| `security/CustomerSessionVerificationException.java` | Stackless sanitized verifier outcomes limited to `UNAUTHENTICATED` and `SERVICE_UNAVAILABLE`. |
| `security/CustomerSessionVerifier.java` | Provider-neutral verifier interface taking only opaque bearer and audience; no production implementation or bypass. |
| `security/OpaqueCustomerBearer.java` | Package-constructed wrapper; deliberate `value()` access for the verifier and redacted `toString()` returning `[REDACTED]`; not stored in principal/application context. |
| `security/VerifiedCustomerSession.java` | Verifier-attested account UUID, channel and audience result; contains no token. |
| `web/CustomerApiErrorController.java` | Web/servlet-only Boot 4 ErrorController marker replacement; ERROR dispatch uses frozen Problems (404/401/503 or sanitized 500); direct requests are denied. Not a business API. |
| `web/CustomerBoundaryFilter.java` | Highest-priority exact route/method/URI boundary; generates request-local trace UUID, authenticates and wraps a principal without ThreadLocal state; rejects other routes/methods with 404. Handles negotiation and buffers responses to prevent partial serialization/error leakage, delegates sanitized failures to the writer. |
| `web/CustomerMeController.java` | Web/servlet-only exact GET mapping producing JSON; accepts only servlet-derived Principal, obtains lazy service after authentication, maps graph initialization to 503, and handles known/unexpected exceptions with frozen Problems. |
| `web/CustomerProblemWriter.java` | Exact `type,title,status,code,traceId` Problem object, `about:blank`, fixed titles/statuses, no detail/exception/path/timestamp. Supports six operation codes plus boundary NOT_FOUND; no-store/no-cache/nosniff headers, bare Bearer challenge only for 401, response reset before sanitized output. Reserved codes are writer vocabulary, not invented enforcement features. |
| `web/CustomerRequestPrincipal.java` | Request-local trusted caller; Principal name is account UUID, never bearer material. |
| `web/CustomerWebConfiguration.java` | Web/servlet-only writer and REQUEST-dispatch filter registration for `/*`, highest precedence; ObjectProvider supplies verifier candidates without adding an adapter. |

`application-web.yml` sets servlet mode explicitly; maps `ONECALL_CUSTOMER_DB_URL`, `ONECALL_CUSTOMER_DB_USERNAME`, `ONECALL_CUSTOMER_DB_PASSWORD` to empty-default customer-only properties; disables request details and Tomcat access logging; turns off jOOQ/Hikari/transaction/JDBC logging and restricts Spring Web to WARN. It adds no default profile, TTL/provider/session policy or PA10 profile inclusion.

The PA10 edit adds the Qualifier import and names every injected sharing datasource/manager/template/DSL/repository within the existing graph. Bean names, profile, parameter types, transaction settings, repository/service implementations and behavior remain unchanged. The real dual-profile integration test verifies separate pools/roles, not merely annotation presence.

POM: adds only `org.springframework.boot:spring-boot-starter-webmvc` at compile scope with no explicit version (Boot 4.1.2 managed), and includes `**/CustomerMeIT.java` in the existing Failsafe container-smoke execution. No MVC-test/security starter, runtime Flyway or alternate engine is introduced. Existing PostgreSQL runtime and jOOQ/Hikari/Spring JDBC compile scopes remain intact.

### Seven new test/support files and two existing extensions

| File (relative to `backend/src/test/java/`) | Evidence responsibility / methods |
| --- | --- |
| `com/onecall/core/customer_iam/internal/security/CustomerSessionBoundaryTests.java` | 5 methods: opaque port/audience, denial equivalence, malformed verifier results, unavailable/ambiguous/missing adapters, secret-wrapper/principal isolation. |
| `com/onecall/core/customer_iam/internal/application/CustomerContextServiceTests.java` | 6 methods: trusted source, frozen projection, missing/disabled account, invalid projection rejection, role-before-repository/read-only transaction, technical/programming sanitization. |
| `com/onecall/archtests/CustomerWebConfigurationTests.java` | 6 methods: default absence, explicit web/missing adapter without DB effects, credential validation, exact dual qualifiers, no profile/classpath verifier bypass, logging/session settings. |
| `com/onecall/archtests/CustomerMeWebTests.java` | 8 actual-HTTP methods: frozen GET JSON, malformed/missing/duplicate bearer, selector spoofing, sanitized Problems/headers, route/method/alias exclusion, concurrent request isolation, negotiation/error dispatch, reserved writer-only vocabulary. |
| `com/onecall/archtests/architecture/CustomerApiBoundaryTests.java` | 3 methods: exact signatures/projections, read-only SQL and dependency boundaries, profile/verifier artifact exclusion. |
| `com/onecall/archtests/container/CustomerMeFixture.java` | Support, zero test methods: disposable PostgreSQL, V001–V009 migration, restricted runtime identities, test-only verifier, JDBC capture/failure injection, snapshots/pool checks and close-managed resources. |
| `com/onecall/archtests/container/CustomerMeIT.java` | 10 methods: migrated role-bound runtime, ACTIVE account, ACTIVE memberships/organizations, deterministic ordering, exact grants/denied locks and columns, next-request lifecycle changes, selector resistance, no-write/cleanup on success and failure, PA10 coexistence, secret-free outage/database failures. |
| Existing `com/onecall/archtests/OneCallApplicationTests.java` | Extends the existing single method with servlet/customer-bean absence; no additional testcase. |
| Existing `com/onecall/archtests/architecture/BootstrapDependencyBoundaryTests.java` | Retains the existing single method; permits/asserts exact MVC/Tomcat/Jackson presence while preserving forbidden-stack checks. Default non-web behavior is proven by contexts and JAR smoke, not absence of web classes. |

The five new unit/web/architecture classes contain exactly `5+6+6+8+3=28` methods; PA11 integration adds 10. Historical 21 Surefire and 74 Failsafe methods remain, producing 49/84/133.

CI now reconciles exact source classes and method sets against XML (not totals alone), checks zero failure/error/skip counters, inspects runtime coordinates and JAR bytes with the exact firewall below, excludes test/verifier-adapter/SecurityFilterChain artifacts, and runs bounded default plus explicit-web executable-JAR smokes with JVM cleanup. README states these capabilities, no-adapter refusal, profile/env separation, current dependencies/scopes and inventory, while retaining labeled historical PA10 results.

## 4. Dependency and firewall evidence

The literal tuple field order is **`group:artifact:type:version:classifier:scope`**. Empty classifier is represented by the empty field before scope, e.g. `commons-logging:commons-logging:jar:1.3.6::compile`. The parser permits hyphenated groups including `commons-logging` and optional classifier syntax; it explicitly checks both Maven's five-field classifier-absent form and the six-field empty-classifier form. Classifier, type, version and scope are identity fields, not silently discarded.

| Inventory | Baseline | Web final | Added / removed |
| --- | --- | --- | --- |
| Unique resolved runtime tuples | 37 | 55 | 18 / 0 |
| Unique packaged `BOOT-INF/lib/*.jar` entries | 34 | 48 | 14 / 0 |

Exact reconciliation is `55 - 8 + 1 = 48`: **47 packaged counterparts of approved resolved tuples plus one exact packager-added JAR**, not a false 55-to-48 bijection. The eight dependency-only coordinates do not approve any packaged JAR:

```text
org.springframework.boot:spring-boot-starter:jar:4.1.2::compile
org.springframework.boot:spring-boot-starter-jackson:jar:4.1.2::compile
org.springframework.boot:spring-boot-starter-logging:jar:4.1.2::compile
org.springframework.boot:spring-boot-starter-tomcat:jar:4.1.2::compile
org.springframework.boot:spring-boot-starter-tomcat-runtime:jar:4.1.2::compile
org.springframework.boot:spring-boot-starter-webmvc:jar:4.1.2::compile
org.springframework.modulith:spring-modulith-apt:jar:2.1.2::compile
org.springframework.modulith:spring-modulith-starter-core:jar:2.1.2::compile
```

The exact 47 tuple-to-filename counterparts, also retained in `/tmp/pa11-web-probe/final-counterparts.txt`, are:

```text
com.zaxxer:HikariCP:jar:7.0.2::compile HikariCP-7.0.2.jar
com.tngtech.archunit:archunit:jar:1.4.2::runtime archunit-1.4.2.jar
org.checkerframework:checker-qual:jar:3.55.1::runtime checker-qual-3.55.1.jar
commons-logging:commons-logging:jar:1.3.6::compile commons-logging-1.3.6.jar
com.fasterxml.jackson.core:jackson-annotations:jar:2.21::compile jackson-annotations-2.21.jar
tools.jackson.core:jackson-core:jar:3.1.7::compile jackson-core-3.1.7.jar
tools.jackson.core:jackson-databind:jar:3.1.7::compile jackson-databind-3.1.7.jar
jakarta.annotation:jakarta.annotation-api:jar:3.0.0::compile jakarta.annotation-api-3.0.0.jar
org.jmolecules:jmolecules-events:jar:2.0.1::compile jmolecules-events-2.0.1.jar
org.jooq:jooq:jar:3.21.9::compile jooq-3.21.9.jar
org.jspecify:jspecify:jar:1.0.1::compile jspecify-1.0.1.jar
org.jmolecules:jstereotype:jar:2.0.1::compile jstereotype-2.0.1.jar
org.slf4j:jul-to-slf4j:jar:2.0.20::compile jul-to-slf4j-2.0.20.jar
org.apache.logging.log4j:log4j-api:jar:2.25.5::compile log4j-api-2.25.5.jar
org.apache.logging.log4j:log4j-to-slf4j:jar:2.25.5::compile log4j-to-slf4j-2.25.5.jar
ch.qos.logback:logback-classic:jar:1.5.38::compile logback-classic-1.5.38.jar
ch.qos.logback:logback-core:jar:1.5.38::compile logback-core-1.5.38.jar
io.micrometer:micrometer-commons:jar:1.17.2::compile micrometer-commons-1.17.2.jar
io.micrometer:micrometer-observation:jar:1.17.2::compile micrometer-observation-1.17.2.jar
org.postgresql:postgresql:jar:42.7.14::runtime postgresql-42.7.14.jar
io.r2dbc:r2dbc-spi:jar:1.0.0.RELEASE::compile r2dbc-spi-1.0.0.RELEASE.jar
org.reactivestreams:reactive-streams:jar:1.0.4::compile reactive-streams-1.0.4.jar
org.slf4j:slf4j-api:jar:2.0.20::compile slf4j-api-2.0.20.jar
org.yaml:snakeyaml:jar:2.6::compile snakeyaml-2.6.jar
org.springframework:spring-aop:jar:7.0.10::compile spring-aop-7.0.10.jar
org.springframework:spring-beans:jar:7.0.10::compile spring-beans-7.0.10.jar
org.springframework.boot:spring-boot:jar:4.1.2::compile spring-boot-4.1.2.jar
org.springframework.boot:spring-boot-autoconfigure:jar:4.1.2::compile spring-boot-autoconfigure-4.1.2.jar
org.springframework.boot:spring-boot-http-converter:jar:4.1.2::compile spring-boot-http-converter-4.1.2.jar
org.springframework.boot:spring-boot-jackson:jar:4.1.2::compile spring-boot-jackson-4.1.2.jar
org.springframework.boot:spring-boot-servlet:jar:4.1.2::compile spring-boot-servlet-4.1.2.jar
org.springframework.boot:spring-boot-tomcat:jar:4.1.2::compile spring-boot-tomcat-4.1.2.jar
org.springframework.boot:spring-boot-web-server:jar:4.1.2::compile spring-boot-web-server-4.1.2.jar
org.springframework.boot:spring-boot-webmvc:jar:4.1.2::compile spring-boot-webmvc-4.1.2.jar
org.springframework:spring-context:jar:7.0.10::compile spring-context-7.0.10.jar
org.springframework:spring-core:jar:7.0.10::compile spring-core-7.0.10.jar
org.springframework:spring-expression:jar:7.0.10::compile spring-expression-7.0.10.jar
org.springframework:spring-jdbc:jar:7.0.10::compile spring-jdbc-7.0.10.jar
org.springframework.modulith:spring-modulith-api:jar:2.1.2::compile spring-modulith-api-2.1.2.jar
org.springframework.modulith:spring-modulith-core:jar:2.1.2::runtime spring-modulith-core-2.1.2.jar
org.springframework.modulith:spring-modulith-moments:jar:2.1.2::compile spring-modulith-moments-2.1.2.jar
org.springframework:spring-tx:jar:7.0.10::compile spring-tx-7.0.10.jar
org.springframework:spring-web:jar:7.0.10::compile spring-web-7.0.10.jar
org.springframework:spring-webmvc:jar:7.0.10::compile spring-webmvc-7.0.10.jar
org.apache.tomcat.embed:tomcat-embed-core:jar:11.0.27::compile tomcat-embed-core-11.0.27.jar
org.apache.tomcat.embed:tomcat-embed-el:jar:11.0.27::compile tomcat-embed-el-11.0.27.jar
org.apache.tomcat.embed:tomcat-embed-websocket:jar:11.0.27::compile tomcat-embed-websocket-11.0.27.jar
```

The only independent packager branch is exact filename equality with `spring-boot-jarmode-tools-4.1.2.jar`, at `BOOT-INF/lib/spring-boot-jarmode-tools-4.1.2.jar`. It is not a resolved project dependency approval and does not allow another tools version/family.

Positive-first validation accepts an exact approved resolved tuple and requires its exact counterpart (or explicit dependency-only absence); packaged validation accepts the exact jarmode branch or an exact counterpart whose approved tuple is actually present. Only then is the unchanged legacy negative regex consulted for nonmembers. **Every nonmember is rejected even when that regex does not match.** Duplicate, malformed, missing, wrong group/version/type/classifier/scope, dependency-only-as-JAR, and missing-counterpart cases fail; final sets must equal the complete literals.

Exactly seven approved packaged entries collide with the legacy regex and pass only through positive membership:

```text
spring-boot-web-server-4.1.2.jar
spring-boot-webmvc-4.1.2.jar
spring-web-7.0.10.jar
spring-webmvc-7.0.10.jar
tomcat-embed-core-11.0.27.jar
tomcat-embed-el-11.0.27.jar
tomcat-embed-websocket-11.0.27.jar
```

Jackson 2 `com.fasterxml.jackson.core:jackson-core` and `jackson-databind` are denied by resolved identity, including forged versions; exact Jackson annotations 2.21 and Jackson 3 `tools.jackson.core` are allowed. Tomcat embedded websocket is infrastructure, not permission for a WebSocket API/starter. Inherited r2dbc-spi/reactive-streams/checker-qual do not authorize reactive drivers. The firewall executes **82 adversarial probes**, including forbidden stacks, forged coordinates, bad parser input, duplicates, omissions and packaging exceptions, and scans product class interfaces for forbidden implementations/test artifacts.

Evidence files under `/tmp/pa11-web-probe/`: `literal-baseline-resolved.txt`, `literal-baseline-packaged.txt`, `final-resolved.txt`, `final-packaged.txt`, `final-dependency-only.txt`, `final-counterparts.txt`, `approved-legacy-collisions.txt`; original/final dependency lists, trees, effective POMs and JAR indexes remain alongside them. The reporter independently compared the actual product ZIP library entries with the final manifest and recomputed the cardinalities/deltas above.

## 5. Final validation evidence and provenance

Build results below are existing execution evidence, not freshly rerun Maven/Docker builds by this reporting task. The reporter read the logs, parsed the current XML, verified all implementation hashes/diff bytes/JAR hash, and re-executed the current CI inventory and firewall Python bodies read-only against current source/XML/JAR, both successfully. No log, XML, target artifact or evidence file was rewritten by reporting.

| Validation | Final result / evidence |
| --- | --- |
| Full `clean verify` with container-smoke profile | `/tmp/pa11-web-probe/full-final.log`: clean goal, Enforcer toolchain/convergence/bans and full build succeed; 49 Surefire + 84 Failsafe = 133, zero failures/errors/skips. Failsafe is 64 PostgresContainerIT + 10 SharingQueryServiceIT + 10 CustomerMeIT. Total recorded time 01:55; original finish timestamp `2026-11-02T06:03:59+08:00` is retained as logged, not converted to the reporter's timezone. |
| Targeted unit/web/configuration/boundary run | `targeted-third.log`: 30 methods, zero failures/errors/skips, BUILD SUCCESS. Earlier `targeted-first.log` has a Boot package test-compilation failure; `targeted-second.log` has two assertion failures (negotiation/error dispatch and secret-wrapper/principal). These are superseded development runs, not green evidence. |
| Targeted CustomerMeIT run | `customer-it-second.log`: 49 Surefire and 10 CustomerMeIT, zero failures/errors/skips, BUILD SUCCESS. `customer-it-first.log` had one sanitization assertion failure; final full run independently passes all 10. |
| Exact CI inventory reconciliation | `final-ci-inventory.log`: source classes/methods and XML inventories match 49 / 64+10+10 = 84 / 133; zero skips/errors/failures. Same current CI Python body independently passes during reporting. |
| Firewall | `ci-firewall.log` and `final-ci-firewall.log`: 37/34 baseline, 55/48 web, 47 counterparts, eight dependency-only, one packager; deltas 18/0 and 14/0, 82 probes and exact seven collisions. Current CI Python body independently reproduces this during reporting. |
| Both executable-JAR smokes | `ci-jar-smokes.log` and `final-ci-jar-smokes.log`: bounded default startup exits successfully without web/DB; explicit web starts on an ephemeral port with no DB/verifier adapter. Missing bearer -> 401, synthetic well-formed bearer -> 503, hostile Accept -> sanitized 401, direct `/error` -> 404. Exact Problem fields/headers/media types, no cookies/pool, unique server trace IDs and child-JVM terminate/kill/wait cleanup are checked. These logs are inspected, not smokes rerun by the reporter. |
| Product bytes | `backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar`, recomputed SHA256 `dbc43d4c8ccf3f05567410b0f1da67e923caf9864c5600b7972feceb8e4d7f80`. |
| Report-time read-only checks | All 33 file SHA256 values match; reconstructed complete diff is byte-identical to evidence; actual ZIP inventory matches final manifest; current XML totals match JSON; protected tracked diff and staged diff are empty; `git diff --check` passes. Git reports autocrlf warnings, not content/whitespace failures. |

The full build is reproducible from `backend` using the pinned Java 21.0.13/Maven 3.9.9 toolchain, e.g. `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke clean verify`. This is a reproduction command, not a claim that the original log records its exact shell invocation. CI uses `./mvnw -B -ntp -Pcontainer-smoke verify`; targeted selection is the corresponding Maven `-Dtest=...` or `-Dit.test=CustomerMeIT` run. Existing logs prove outcomes; this report does not invent missing shell command transcripts or remote CI results.

### PostgreSQL transaction, ACL, no-write and cleanup proof

`full-final.log` records `PA11 PostgreSQL version=16.15 collation=C zone=Asia/Shanghai`, image `postgres:16`, image ID **`sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`**, startup 6319 ms. The fixture asserts a PostgreSQL 16 server, migrates and validates all nine existing migrations, and prints the inspected image ID; PA10's final run independently records the same server/image. This is the observed local image, not a new production provisioning guarantee or immutable-tag promise.

- A successful request records exactly fixed role switch, effective-role assertion and projection SELECT on one connection checkout/backend PID. Captured facts assert effective `customer_iam_role`, restricted login as `session_user`, read-only `on`, READ COMMITTED and exact indexed bindings. Runtime login is non-superuser and NOINHERIT; customer and sharing identities cannot assume each other's role (42501).
- ACL tests permit only existing V002 projection grants, enumerate allowed organization/membership columns and deny foreign/sensitive columns and row-lock access. No schema, grant or role migration is added.
- ACTIVE account, membership and organization state changes committed between requests are observed on the next request; deterministic PERSONAL-first/UUID ordering and all allowed organization roles are tested. No lease/cache consistency claim extends past the read transaction.
- Success, injected SELECT/role/commit failures and a deliberate write probe are exercised. The UPDATE probe fails with PostgreSQL SQLSTATE `25006`; at least three rollbacks are asserted. SQL capture admits only `SET LOCAL ROLE customer_iam_role`, `select current_user` and the projection SELECT.
- Before/after canonical snapshots match for `customer_iam.customer_account`, `customer_iam.auth_identity`, `customer_iam.identity_profile`, `organization_access.organization`, `organization_access.organization_membership`, `platform.idempotency_record`, `platform.outbox_event` and `audit.audit_fact`. No login timestamp, identity, idempotency, outbox or audit write is hidden in `/me`.
- After success/failure, pooled connections report current/session user restored to their login, transaction read-only `off`, READ COMMITTED, autocommit true and zero active checkouts; the backend is reused safely after injected errors. Dual web/PA10 contexts have distinct DSL/template/pools and no role leakage.
- Fixture cleanup closes contexts, all tracked Hikari pools, HTTP client, test session state and PostgreSQL container in finalization; HTTP concurrency uses bounded executor cleanup. JAR smoke has bounded process teardown. The reporter launches no DB/container/server and leaves no such runtime to clean up.

### Hash manifest, complete diff and test XML inventories

All paths below are local evidence, not externally published artifacts:

| Evidence | Recomputed SHA256 / interpretation |
| --- | --- |
| `/tmp/pa11-web-probe/implementation-product-test-ci-readme.diff` | `4676ea1a98c608765d81e22a1e73694461e1520425f57f53687956b1d82dbbe4`; complete 33-file diff, reconstructed from `git diff HEAD --` followed by sorted untracked `git diff --no-index -- /dev/null <file>` output. |
| `/tmp/pa11-web-probe/implementation-files-sha256.json` | `ec17ca3eac55b04436f01525d5d651d51015f62f29f49afee8bd271ba200b5fb`; all 33 current file hashes verified, not trusted from prose. |
| `/tmp/pa11-web-probe/implementation-changed-files.txt` | `9a21515ac5452d436cd12b79b983dfa59bececb9a0aa12e8b4440eb478d47b9e`; exact list above. |
| `/tmp/pa11-web-probe/final-test-inventory.json` | `7d59c85aa4840c482bfa65a8fea23efef433b5f8349dd5374e588efb314ea752`; 11 Surefire and three Failsafe XML paths/counters independently parsed and reconciled. |

Exact final XML paths:

```text
backend/target/surefire-reports/TEST-com.onecall.archtests.CustomerMeWebTests.xml
backend/target/surefire-reports/TEST-com.onecall.archtests.CustomerWebConfigurationTests.xml
backend/target/surefire-reports/TEST-com.onecall.archtests.OneCallApplicationTests.xml
backend/target/surefire-reports/TEST-com.onecall.archtests.SharingPersistenceConfigurationTests.xml
backend/target/surefire-reports/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml
backend/target/surefire-reports/TEST-com.onecall.archtests.architecture.CustomerApiBoundaryTests.xml
backend/target/surefire-reports/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml
backend/target/surefire-reports/TEST-com.onecall.archtests.architecture.SharingQueryBoundaryTests.xml
backend/target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests.xml
backend/target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests.xml
backend/target/surefire-reports/TEST-com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests.xml
backend/target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerMeIT.xml
backend/target/failsafe-reports/TEST-com.onecall.archtests.container.PostgresContainerIT.xml
backend/target/failsafe-reports/TEST-com.onecall.archtests.container.SharingQueryServiceIT.xml
```

## 6. Canonical contract baseline residuals (not green)

`/tmp/pa11-web-probe/contract-results.txt` preserves exact validator commands, working directories, exit statuses and log paths. Node 22.23.4 assertion and locked A1 toolchain installation pass. A1 S3 exits 0 (`contracts-a1-s3.log`), A3 S3 exits 0 (`contracts-a3-s3.log`) and A4 S2 exits 0 (`contracts-a4-s2.log`). The following remain explicitly failed:

| Residual | Existing evidence / classification | Owner and destination |
| --- | --- | --- |
| A2 S3 protocol validation | Exit 1, `contracts-a2-s3.log`: fixtures 25/25 and A1 drift 0 do not make the validator green. It reports existing A0 column dictionary/named CHECK drift, including `a0_status_drift=8`, `a0_registry_drift=6`, `a0_column_drift=40`, missing named CHECKs and missing A0 dictionaries/column references; final `FAIL protocol-validation (s3)`. Pre-existing contract baseline residual. | A0 data-model and A2 protocol contract owners; destination: separate canonical A0 dictionary/check versus A2 reference reconciliation work, coordinated by project controller. Not a PA11 source workaround. |
| A5 S1 and S2 acceptance validation | Both exit 2; `contracts-a5-s1.log` and `contracts-a5-s2.log`: `FAIL acceptance-validation (invalid-arguments)` and exact assertion `state table header changed in section 7.1`. Pre-existing contract baseline residual, not an acceptance pass. | A5 acceptance-matrix/documentation owner with project controller; destination: separate section 7.1 document/parser baseline reconciliation and rerun of S1/S2. |

Protected-source `git diff HEAD --` is empty for `docs`, `api`, `protocol`, `security`, `acceptance`, `deployment`, default/persistence resources and migration resources before report creation. In particular, A0 `docs/14-production-data-model-and-flyway-schema-spec.md`, A2 `docs/16-production-ivr-protocol-spec.md` plus `protocol/`, and A5 `docs/19-production-acceptance-matrix.md` plus `acceptance/` have empty tracked diffs against the start HEAD. These files were not modified by PA11. At reporting completion the only new docs file is this report; tracked protected-source diff remains empty. Their unchanged HEAD identity and existing logs support the baseline classification; no canonical-validator repair or fresh validator rerun is claimed by this reporting task.

The historical `pa11-customer-auth-api-foundation-implementation.md` remains unchanged evidence of the earlier strict-bijection blocker. Accepted fix-3 replaces that impossible requirement with the exact two-inventory contract now implemented; this report does not rewrite history or describe A2/A5 as resolved.

## 7. Security, exclusions and remaining ownership

Bearer parsing is opaque/customer-audience-only; invalid/revoked/expired/disabled session outcomes are provided through the verifier contract and tested using test-only implementations. There is no production issuer/store/revocation implementation, fake-token route, test/dev-profile bypass or manufactured authentication. Principal/application objects contain only trusted UUID/channel, and bearer wrapper stringification is redacted. Raw exceptions, SQL, bindings, identities, tokens, credentials and provider subjects are excluded from response Problems and relevant logs; failure tests exercise log capture and sanitization. This report includes no credential or raw bearer value. Malformed connector-level HTTP is outside the application Problem contract.

There is **no login, no provider, no Redis and no other business API**. No session issuance/refresh/logout, cookie-based authentication, mutation, migration, schema/index/ACL change, external provider/cloud call, frontend or provisioning is added. `/error` is internal error dispatch with direct-request denial, not a second business operation. No production request writes to account/identity, audit, outbox or idempotency tables. Denial auditing is deferred rather than silently introduced as a hidden write.

| Remaining risk / limitation | Named owner and destination |
| --- | --- |
| Production authenticated success deliberately unavailable without exactly one real verifier; issuer/provider/channel provenance, revocation/store/TTL and identity lifecycle remain future work | Identity/Backend/Security owners; separately approved customer authentication adapter/session work unit, not S1 completion inflation. |
| A2 A0 dictionary/CHECK drift and A5 section 7.1 header assertion remain failed | A0/A2 contract owners and A5 acceptance owner respectively; canonical baseline repair destinations in section 6, controller tracking required. |
| Advisory workspace read can race later lifecycle changes; not an authorization lease | Backend/Domain/Security owners; downstream operation authorization must recheck its own current facts. |
| Production login grants, DB provisioning, image pinning/collation, pool sizing/timeouts/capacity and edge/external logging not proven by disposable tests | Platform/Database/Security owners; deployment/capacity Q17/Q23 and production provisioning/hardening work. Hikari vendor defaults are not measured SLOs. |
| Denial audit and wider abuse/rate-limit enforcement are not implemented merely because Problem vocabulary includes reserved codes | Security/Identity owners; future approved auditing/edge policy work respecting the no-hidden-write boundary. |
| `/tmp` evidence and target XML/JAR are local/ephemeral; no remote CI or review acceptance attested | Backend-CI/project controller; preserve these exact hashed artifacts in the authorized evidence workflow before cleanup, then run separately authorized review/CI gates. |

No source, test, plan, controller, README, CI, POM, review artifact or existing evidence was edited by this reporting task. No staging, commit, push, branch/PR operation, approval, readiness marking, reviewer request, external comment or review-gate entry was performed. Implementation completion is the bounded PA11 S1 outcome, not contract-baseline green, accepted review or production readiness.

status: implementation-complete
