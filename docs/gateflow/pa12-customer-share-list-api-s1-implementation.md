# PA12 S1 implementation evidence

- Task: `pa12-customer-share-list-api-s1-completion-fix-gpt-20261102-175500`.
- Gate: implementation; changes remain uncommitted and unstaged.
- Accepted plan: `docs/gateflow/pa12-customer-share-list-api-plan.md`, SHA256 `b4dfc1789c0783996ec92189328b607456d1e8364ab5c68c91adf266de81f13a`.
- Outcome: implementation remains blocked; the required assertion gaps below are not waived by green aggregate builds.

## Bounded fix

Removed the Tomcat `allowTrace` customizer and its imports from `CustomerWebConfiguration.java`; no production HTTP behavior customization remains there. `onlyTwoExactGetPathsPassHighestPriorityFilter` now tests TRACE directly against the actual `CustomerBoundaryFilter` with a mock servlet request/response and verifies the filter chain is not called. The network TRACE request was removed from that test's method loop because Tomcat rejects TRACE before application-filter dispatch when the connector retains its production default.

## Validation evidence

All Java/Maven commands used `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\ProgramData\ONECALL-no-such-unix-socket-dir`.

- Focused `CustomerSharesWebTests,CustomerWebConfigurationTests,CustomerMeWebTests`: 24 tests passed, zero failures/errors/skips.
- `mvn clean verify`: passed; 67 Surefire tests, zero failures/errors/skips.
- `mvn -Pcontainer-smoke -Dit.test=CustomerSharesIT verify`: passed; `CustomerSharesIT` 8/8.
- `mvn clean -Pcontainer-smoke verify`: passed; 67 Surefire and 92 Failsafe tests, zero failures/errors/skips.
- Latest XML after the full container-smoke build: Surefire 13 files / 67 tests; Failsafe 4 files / 92 tests; 159 total; zero failures/errors/skips. This confirms aggregate inventory counts, not every requested semantic assertion.
- `git diff --check`: passed. Git diff over protected V001–V009, OpenAPI, docs/11/12/14/15/17 and `application*.yml` paths was empty.
- The application workspace remains uncommitted and unstaged; no commit, push, PR, or review was started.

## Residual acceptance gaps

- `CustomerSharesWebTests` still needs the exact programmatic `SpringApplicationBuilder(OneCallApplication.class, ...)` context setup with test-owned verification/service mocks, and complete `/pa12` path/alias/non-GET/error/unprefixed/call-count assertions. Existing context coverage is not that specified pattern. Complete bearer/provider missing, ambiguous, outage and malformed cases with the required zero-call assertions; verify exact missing/ambiguous query-provider behavior; add the dedicated serializer-fault context inside the existing negotiation method. The fault target must be wire type `CustomerSharePage.Item`, not internal `ShareSummary`. The implementation-time reconciliation is that the controller maps `ShareSummary` to `CustomerSharePage.Item` before serialization; this does not change the API.
- `CustomerSharesWebTests` also needs the specified security/header and Accept/selection-precedence assertion completeness retained alongside its existing no-production-adapter and database-free checks.
- `CustomerWebConfigurationTests` still needs absent sharing graph/provider checks in web-only mode. Its qualifier test exists, but must be reconciled against and prove every exact customer/sharing qualifier while retaining exactly six test methods.
- `CustomerMeWebTests` does not yet prove every legacy/selected/invalid workspace structure, exact unchanged response/order/status, and zero sharing-provider resolution within the existing methods.
- `CustomerSharesIT` / `CustomerSharesFixture` do not yet establish the entire required metadata/ACL-column matrix, independent role/pool facts, read-only and read-committed facts, complete before/after snapshots over every required table and secret canaries, complete failure/status matrix, pool restoration after each success/failure, or all mapping/statement/disconnect/commit failure paths with test-owned hooks. Do not claim cross-transaction write injection. Existing eight method names and fresh-per-method identity/cleanup behavior must be preserved when closing these gaps.
- The exact CI inventory body, JAR smokes, 82-probe production-JAR firewall, complete contract-validator sequence, protected-path hash comparison, and final no-out-of-scope production-path audit were not rerun in this completion-fix pass. Earlier artifact evidence records an exact inventory-body Windows separator mismatch (the `as_posix()` equivalent passed), A5 s1/s2 failures against a changed protected input header, and toolchain deviations (JDK 21.0.12.1; Node 20.20.1 rather than pinned requirements). These are not asserted as freshly verified here; the A5 failures require preserving the protected input and reporting the input mismatch, not editing it.

The current diff contains only the requested removal in `CustomerWebConfiguration.java` and the direct filter-level TRACE assertion in `CustomerSharesWebTests.java`, in addition to the pre-existing PA12 worktree changes. `CustomerWebConfiguration.java` now matches base commit `c49d241`.

status: s1-implementation-blocked
