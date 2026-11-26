# PA43 S1 implementation retry — required-path stop

## 1. Identity and disposition

- Task: `pa43-s1-implementation-gpt-20261126-183000-retry1`; date: 2026-11-26.
- Work unit: `PA43-operations-order-read-foundation`; gate: `implementation`; single end-to-end read-only slice S1.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa43-next-work-unit/codex-call`.
- Branch: `codex/pa43-next-work-unit`.
- Verified start HEAD: `aa99cfe1e0675c7e9a277fa754a12f3ff58349cd`.
- Verified `origin/main`: `29e715a3dccd8bc3fbedb4a094555294b7482a67`.
- Accepted plan: `docs/gateflow/pa43-operations-order-read-plan.md`, 359 lines / 64,245 bytes / raw SHA256 `62622b5ab3b92897fa716cd8caa3ef23eadd0f1acd3819c5e2029cbb4d8276a9`.
- Retry Control raw SHA256: `b0388083b80ccbc1d9d92319499aebb4eeb0d9cb65a34861ba689167578752fa`, independently matched before work and after the blocker.
- Read inputs: active PA43 Control checkpoint, the complete accepted plan, all six accepted plan review/re-review artifacts, and relevant source/guard/configuration evidence.

**BLOCKED / NOT IMPLEMENTED.** Attempt 1's Docker-access blocker is resolved in this execution context. A different, source-backed required-path blocker was discovered before any product edit. Dispatch and accepted-plan §9 explicitly require stopping when a necessary path is outside the exhaustive allowlist. This report replaces the prior attempt's report; it is not a successful implementation claim.

Exact authored changed-path set: `{docs/gateflow/pa43-operations-order-read-s1-implementation.md}`. This is the sole additional Gateflow path explicitly authorized by dispatch. None of the 33 product/test/build paths was changed. The pre-existing modified Control and untracked private helpers were preserved; Control's raw hash is still the dispatch hash. Maven generated ordinary ignored `backend/target` outputs only; no private helper or generated runner directory was authored.

## 2. Required-path blocker and minimum decision

**SCOPE-CUSTOMER-FILTER-01**

Exact newly required path:

`backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java`

This path is not one of the 33 authorized product/test/build paths. Existing customer source is also expressly frozen by accepted-plan §2 and the dispatch.

Direct evidence:

1. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java:55` registers this filter for `/*`, REQUEST dispatch, order `Ordered.HIGHEST_PRECEDENCE + 1` (lines 59–61). Thus it runs after the operations boundary, not only for customer URLs.
2. `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsWebConfiguration.java:24` registers the operations boundary for `/api/v1/operations/*` at `Ordered.HIGHEST_PRECEDENCE`. Successful employee authentication there still chains into the global customer filter.
3. `CustomerBoundaryFilter.java:67` explicitly passes through the existing exact operations GET templates: subjects, activations, IVR sessions and catalog products. Neither operations order route is included.
4. `CustomerBoundaryFilter.java:82` admits only specified customer paths in its subsequent gate. At lines 106–109 an operations order GET fails that gate, writes generic 404 and returns without invoking the downstream chain. A trusted employee principal or bearer header does not change this branch.
5. Accepted-plan §9 authorizes changing only `OperationsBoundaryFilter.java` among existing production paths and says existing registration needs no change. Adding the two order paths there alone cannot make either endpoint reachable through the unchanged global customer filter.

Runtime reproduction used the compiled, unchanged production `CustomerBoundaryFilter`, Spring `MockHttpServletRequest`/`MockHttpServletResponse`, a supplied employee principal, a bearer header and a downstream invocation flag. It did not mock the filter implementation, alter source, register an alternative filter or disable constraints.

A second reproduction also supplied the actual `OperationsRequestPrincipal` created from the existing `EmployeeBearerAuthenticator` and a synthetic successful `EmployeeSessionVerifier`. Both order paths still returned 404 with `downstream=false`; the catalog-products control passed through. It printed `assertions=3 passed=3` and exited 0. This establishes that successful employee-session authentication does not overcome the downstream route gate. It is a servlet-filter runtime probe, not an implemented endpoint or socket-level HTTP test.

| GET path | Status | Downstream invoked |
| --- | --- | --- |
| `/api/v1/operations/orders` | 404 NOT_FOUND | false |
| `/api/v1/operations/orders/00000000-0000-0000-0000-000000000001` | 404 NOT_FOUND | false |
| `/api/v1/operations/catalog/products` (existing sibling control) | 200 | true |

All three probe assertions completed without a JShell error; launcher exit 0. This is an in-process servlet-filter runtime reproduction, **not** a real-HTTP PA43 success test. The source registration proves why this filter participates in real requests.

Minimum decision required from Controller / customer boundary owner: explicitly reconcile the frozen-customer-source rule and exhaustive allowlist with the need for exact employee order GET pass-through in this one file, preserving customer behavior, employee authentication, URI equality and denial of aliases/other methods. Any authorization should retain one end-to-end slice and add corresponding negative/positive regression obligations. No schema, migration, role, grant, pool or contract change is indicated.

No workaround was implemented: no URI masquerading as a sibling endpoint, filter removal/reordering, alternate dispatch, customer-session reuse, programmatic registration bypass or weakened HTTP assertion. The author does not expand the allowlist.

## 3. Changed paths and call-path status

Exactly one authored repository path:

`docs/gateflow/pa43-operations-order-read-s1-implementation.md`

It is the additional artifact explicitly authorized by the dispatch. All 33 product/test/build paths remain unchanged. The modified Control and untracked `.codex` / `.pa43-*` helpers pre-existed this turn and were not edited, removed or staged.

Neither operation is implemented:

- `GET /api/v1/operations/orders` / `listOperationsOrders`.
- `GET /api/v1/operations/orders/{orderId}` / `getOperationsOrder`.

The required future call path remains canonical route gate → trusted employee session → existing read-only repeatable-read transaction → current employee authority → request parsing → list-only endpoint-authenticated cursor → catalog-owner order read/validated historical snapshot → entitlement-owner complete issuance set → immutable exact projection → transaction and role cleanup → serialization. No new production state transition exists in this attempt.

## 4. Exact environment and validation commands

Git commands used:

```bash
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call2
export GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa43-next-work-unit/codex-call
```

Maven executions after the initial PATH diagnostic used the existing Linux toolchain, without installation or upgrade:

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:$PATH
cd backend
```

| Exact command | Exit | Actual result |
| --- | --- | --- |
| `docker version` | 0 | Client and server 29.1.3; API 1.52; Docker server reachable. |
| `docker image inspect postgres:16` | 0 | Cached image ID and repository digest `sha256:3950ad7c3148ca2175ef1b6c36c9efbf78957ae8fbe8b3a5d7c35af50500382f`; image environment PG_VERSION `16.16-1.pgdg13+1`. |
| `java -version` (original PATH) | 0 | Windows-path Temurin 21.0.12.1; not used for Maven validation. |
| `./mvnw -version` (original PATH, from backend) | 1 | Launcher reports `ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher`. Corrected by selecting the already installed Linux JDK, not by changing repository files. |
| `./mvnw -B -ntp --version` (Linux JDK) | 0 | Maven 3.9.9, Eclipse Adoptium Java 21.0.13, runtime `/opt/jdk21`. |
| `./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsSubjectReadBoundaryTests -Dit.test=OperationsIvrSessionReadsIT verify` | 0 | BUILD SUCCESS; 8 unit tests plus 24 PostgreSQL integration tests; 0 failures, 0 errors, 0 skipped. |
| `./mvnw -B -ntp -Pcontainer-smoke clean verify` | 143 | Baseline run intentionally terminated with SIGTERM once the mandatory scope stop was demonstrated; **not full verify green**, not a test failure diagnosis. |
| `kill -TERM 1978388` | 0 | Stops the baseline Maven process; subsequent process check shows Maven and forked Surefire processes gone. |
| `jshell --class-path "$CLASSPATH"` with the reproduction below | 0 | Both order paths are blocked; sibling control reaches downstream. |
| `git diff --cached --exit-code` | 0 | No staged changes. |
| `git diff --check` | 0 before and after report replacement | No whitespace defect in tracked working changes. |
| `git diff --exit-code HEAD -- backend .github/workflows/ci.yml api/openapi` | 0 | No product/build/contract changes. |
| `git diff --no-index --check /dev/null docs/gateflow/pa43-operations-order-read-s1-implementation.md` | 1 | New-file comparison; emits a line-ending warning, no whitespace diagnostic. Not represented as a zero-exit check. |

A separate Python standard-library assertion checked that this report ends with LF and that every line equals its trailing-space/tab-stripped form; it printed `report_whitespace_assertions=passed` and exited 0.

The full baseline run was started before the blocker was found, solely to establish unchanged repository runtime health. Its completed XML is partial; it must not be used for final inventory acceptance. The earlier focused reports were removed by this run's `clean`; the 8+24 success result is supported by that completed invocation's console output, not falsely claimed as retained final XML.

Probe command construction: Python's standard XML parser read the `java.class.path` property from an existing `backend/target/surefire-reports/TEST-*.xml`; that value was passed unchanged to `/opt/jdk21/bin/jshell`. The JShell input was:

```java
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.customer_iam.internal.security.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.*;
import java.util.concurrent.atomic.AtomicBoolean;
var beans = new DefaultListableBeanFactory();
var filter = new CustomerBoundaryFilter(
    new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)),
    new CustomerProblemWriter());
for (String path : java.util.List.of(
    "/api/v1/operations/orders",
    "/api/v1/operations/orders/00000000-0000-0000-0000-000000000001",
    "/api/v1/operations/catalog/products")) {
    var request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    request.addHeader("Authorization", "Bearer employee-test-only");
    request.setUserPrincipal(() -> "employee-test-only");
    var response = new MockHttpServletResponse();
    var called = new AtomicBoolean();
    filter.doFilter(request, response, (incoming, outgoing) -> called.set(true));
    System.out.println("PA43_GLOBAL_FILTER_PROBE path=" + path
        + " status=" + response.getStatus() + " downstream=" + called.get()
        + " body=" + response.getContentAsString());
    if (path.contains("/orders") && (response.getStatus() != 404 || called.get()))
        throw new AssertionError("Unexpected order gate result");
    if (path.contains("/catalog/products") && !called.get())
        throw new AssertionError("Sibling pass-through control failed");
}
/exit
```

The second invocation used the same classpath command and loop, replacing the lambda principal with the following actual employee boundary setup:

```java
import com.onecall.core.operations_iam.internal.web.OperationsRequestPrincipal;
import com.onecall.core.operations_iam.internal.security.*;
EmployeeSessionVerifier verifier = (bearer, audience) -> new VerifiedEmployeeSession(
    java.util.UUID.fromString("00000000-0000-0000-0000-000000000043"),
    EmployeeBearerAuthenticator.AUDIENCE);
beans.registerSingleton("employeeVerifier", verifier);
var employeeAuthenticator = new EmployeeBearerAuthenticator(
    beans.getBeanProvider(EmployeeSessionVerifier.class));
```

Inside the loop it used:

```java
request.setUserPrincipal(new OperationsRequestPrincipal(
    employeeAuthenticator.authenticate(request.getHeaders("Authorization"))));
```

It printed each path, principal class, status and downstream flag, used `System.exit(1)` for either unexpected result, and printed the three-passing-assertions marker after the loop.

No PA43 focused unit/HTTP/configuration/IT classes were created, so the accepted plan's new-class focused commands were not run. No required PostgreSQL test was skipped or represented as passed; implementation acceptance is blocked before those tests exist.

After termination, the fresh partial Surefire XML contained 33 completed suites / 260 tests / 0 failures / 0 errors / 0 skipped. Fresh Failsafe XML contained zero suites because that phase was not reached. These 260 completed baseline tests are separate from the earlier 8+24 focused invocation; they are not a complete suite or 292 unique-test acceptance claim.

## 5. PostgreSQL16 and evidence limits

The completed baseline invocation reached Docker through Testcontainers 2.0.5, started Ryuk and cached `postgres:16`, connected to **PostgreSQL 16.16**, validated all 20 migrations, applied V001–V020 successfully to an empty database and validated them again. Existing `OperationsIvrSessionReadsIT` completed all 24 tests with zero failures/errors/skips. Its console emitted existing PA35 IVR EXPLAIN output.

This is actual baseline runtime evidence, not PA43 evidence. In particular:

- No PA43 owner SQL, authority/filter/cursor/projection or employee-order real-HTTP campaign ran.
- No PA43 LO ACL/no-write/role-restoration/pool-reuse fault campaign ran.
- No PA43 before/after business-row/sequence/ACL/owner/membership/settings inventory was collected.
- No 10,001-order / 10,000-issuance PA43 seed or prepared order/issuance EXPLAIN ran.
- Existing baseline IVR EXPLAIN output is not a substitute for order EXPLAIN.
- No production/live deployment, real provider, key provisioning or external service validation occurred.

## 6. Frozen inputs, inventory, package and firewall

No production/test/build/README byte was edited. Source layout remains 332 production Java files and 216 test Java files; the POM remains at 46 Failsafe includes. No new test identities, methods or annotation hits were added and no current/historical pin was changed.

The frozen-input measurement compares every enumerated file's CRLF→LF bytes with its HEAD blob (also LF-normalized), and computes separate raw and canonical-LF manifests. Manifest algorithm: sorted repository-relative UTF-8 path, NUL, lowercase ASCII SHA256 of member bytes, LF; SHA256 of the concatenation. This diagnostic does not replace any repository guard algorithm.

The Python standard-library measurement exited 0; every member's canonical comparison with HEAD passed:

| Group | Files | Raw manifest SHA256 | Canonical-LF manifest SHA256 |
| --- | --- | --- | --- |
| V001–V020 | 20 | `89066d63528b183f3b574ec2b111d2383ac0ba92e09950ff51e526cc720d08a7` | `247a683f733b421178903bac37a8123db022d4799b09111da799d250e25a5217` |
| OpenAPI | 10 | `b713977c7cf8f44dce33029d0ae73fefdf7202916ede0b9295406568cb4f6779` | `d9b62fe1aaf0cc71543757caa4a5c4416bf4996f54ca4b650d99ee99a7f6ad77` |
| docs12/14/15/17 | 4 | `976ba5c48c37b7deaff3d4e9774298086813e2161cdc36abba6fe39b0017b089` | `ddf8b4d87a187edbb7950b90f72b9806092b0319aa37bd9445d59e3f4e2aaeb9` |
| PA25 | 1 | `0f392c422a8f46330f27af1394071945b57cb2e5ea39ace6e800feb205f10e06` | `c07b6e90bf4393ebf21c9b212b1423705905f3e7fccf9099404e95ecc94953ac` |
| All production Java | 332 | `ff802e8c2ff5972f841ca58fa813e9188ea28372f3ff4ef77ba4be621ffb805b` | `02a15446bb55e0c095bbc991a8dbe759d59e2dbc8c3ff8d31bdd661e2708d740` |
| Customer-IAM Java | 60 | `6ca9c25862b54773eb8f9f6dd7eda52592e50ad559e2d5fbf91318596d30e673` | `05389de0276cdc491fd0a68e5a40bf0e16d7472ae999243a6ceaf740738e4ca1` |

No final PA43 source/CI/POM/XML equality or historical reconstruction is claimed. The full run stopped; its reports are incomplete. No post-implementation executable JAR exists. The earlier focused baseline verify packaged the unchanged application, but subsequent `clean` removed that package. No package hash/parity, baseline/SSM comparison, dependency-firewall runtime listing or adversarial package probes were completed for PA43. No dependency or plugin change was made.

## 7. Documentation decision, residuals and continuation

Only this authorized implementation artifact changes. README remains unchanged because the new capability and final measured passing inventory do not exist. Its append-only insertion interval, historical/FROZEN bytes, four controlled path tokens, current LF slots and composites remain untouched. Design/OpenAPI/Control/PA25/runbook documents are not modified.

Blocking destination: **Controller / customer boundary owner**, to decide the exact newly required source-path authorization and reconcile it with the frozen customer-source rule. The minimum correction is route pass-through, not new customer functionality; it is nevertheless outside this author's current authority.

All implementation and acceptance obligations remain with Backend/Test Maintenance after that decision. Backend/Database retains global-keyset and issuance-fan-out EXPLAIN/deadline assessment. Security/Database retains existing command-capable owner-role amplification; Security/Backend retains per-request revocation semantics; Security/Operations retains cursor key custody/rotation and production deployment validation. These are not resolved by baseline tests.

Completion status: **STOPPED AT REQUIRED-PATH INTAKE; S1 NOT COMPLETE**. No source outside the allowlist changed, no customer behavior changed, and no contract/migration/PA25 modification occurred. No speculative hardening or unrelated repair was made.

No stage, commit, amend, push, PR creation/update, merge, approval, ready-state change, reviewer request, external comment, issue mutation, branch creation/deletion or code-review action occurred.
