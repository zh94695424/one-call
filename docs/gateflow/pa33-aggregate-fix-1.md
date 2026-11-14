# PA33 aggregate fix 1

## 1. Authority and runner

- Task: `pa33-aggregate-fix1-gpt-20261114-01`; date: **2026-11-14**, Asia/Shanghai; gate: bounded aggregate fix for fresh independent A/B re-review.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa33-next-work-unit/codex-call`.
- Measured unchanged HEAD/base: `8e0ec184d0584b75cd1929873d8f895b2e5267ef`; accepted slice: `5cec8a6da16c3531b8e21afadee85144320641a3`.
- Required dispatch: Codex / GPT `gpt-6.1-sol`, reasoning `high`. This records the required dispatch, not independent runtime/model attestation; Controller owns runner-metadata verification. No provider switch or delegation is claimed or performed.
- Inputs: accepted PA33 Control tail, aggregate A `docs/reviews/code-review-20261114-205251.md`, aggregate B `docs/reviews/code-review-20261114-222844.md`, current implementation report and directly involved production/tests. The cumulative plan is not rewritten or dumped.

## 2. Finding disposition

| Finding | Fix-author result | Evidence and boundary |
| --- | --- | --- |
| **PA33-AGG-01** | **Fixed; pending independent re-review** | Real monotonic five-second request budget in the existing service; deterministic fixed-identity regression; sanitized 503, rollback, no partial response. |
| **PA33-AGG-02** | **Fixed; pending independent re-review** | Known owner failures retain codes; infrastructure/structural failures stay 503; unknown mock-port runtime exceptions become sanitized 500 on both GETs. |
| **PA33-AGG-04** | **Authorized bounded portion fixed; pending independent re-review** | One Operations-owned role-name source used for SET LOCAL ROLE and assertRole; five explicit Failure/Code imports. No policy/type relocation or ownership/interface change. |
| **PA33-AGG-03** | **Deferred, untouched** | Detail A0 model metadata omission remains with Contract/OpenAPI and Security/Data classification in a separately authorized contract work unit. Frozen OpenAPI/docs/14/15/17 are not edited. |

### Request budget

The existing public constructor delegates to a package-private `LongSupplier` constructor using `System::nanoTime`. Each `read()` captures its own deadline before entering `TransactionTemplate.execute`, so checkout/transaction setup is included and concurrent or successive reads do not share mutable deadline state. Deadline arithmetic uses subtraction and is tested across signed nanosecond-counter wraparound; no workstation or database wall-clock timestamp decides budget exhaustion.

Checks occur after acquisition, after context entry, around authority queries, before and after every owner call, at each activation-filter batch, after projection assembly and after successful context cleanup while still inside the transaction callback. Exhaustion throws the existing sanitized `Failure(Code.SERVICE_UNAVAILABLE)`, causing transaction rollback before any response escapes. Already accumulated matches are discarded. A successful cleanup cannot turn an expired response into a committed success.

The existing transaction timeout 5, `statement_timeout=2000ms`, `lock_timeout=250ms`, `idle_in_transaction_session_timeout=5000ms`, 100-batch/10,100-candidate ceiling and 100-entry detail-history limits are unchanged. No production file/API, database statement, grant, schema, migration, dependency, public wire contract or timeout-setting path is added.

This is synchronous cooperative deadline enforcement at operation boundaries, not an asynchronous watchdog or statement interruption. An in-flight owner operation or required role/connection cleanup may finish after five seconds before the next check observes exhaustion. It cannot start another owner operation/batch after a budget check observes expiry; the unchanged database per-statement limits remain active. This fix does not claim an exact five-second HTTP-delivery or physical-connection-release SLA.

### Existing fixed regressions

Only these existing annotated methods receive new assertions; none is added or renamed:

- `OperationsSubjectReadServiceTests.candidateBudgetExhaustionNeverInventsTailOrContinuation`: retains deterministic 100-batch capacity failure with a stationary clock; advances candidate or activation calls by one second and proves termination at the fifth batch rather than 100, including a previously matched row that must not escape; asserts exact call counts, rollback/no commit, context exit and connection close. Also checks exact-boundary exhaustion during entry, authority, detail, trial, evidence, order and cleanup; verifies downstream owners are not called after earlier exhaustion. Two successful reads each taking 4,999,999,999 synthetic nanoseconds prove the nonexpired boundary and per-request budget reset. No sleeping or elapsed-workstation-time assertion is used.
- `OperationsSubjectReadServiceTests.sqlBeanTransactionAndUnknownFailuresMapWithoutLeakingCauses`: uses the real service/controller with mocked authority or subject owner ports for both list and detail. jOOQ/Spring data, transaction and bean failures produce 503; ordinary `IllegalStateException` produces INTERNAL/500; known NOT_FOUND remains 404 and known SERVICE_UNAVAILABLE remains 503. Null owner structure produces 503. Exact Problem property names, status/code, UUID trace, no cause/class/canary/SQLSTATE leakage, privacy headers, no cookies and rollback/no commit/close are asserted.

The fixture gains only a constructor overload for the package-private service clock seam. No annotated test identity, test class or production path is created. The role suite continues to exercise real database handoff/restoration, least privilege, resource identity, cleanup and zero-write behavior with the consolidated literals.

## 3. Validation and runner results

Native runtime: **Eclipse Temurin 21.0.13+9**, **Apache Maven 3.9.9**, explicit repository `/root/.m2/repository`; Docker Server **29.1.3**; focused IT database **PostgreSQL 16.15** with exactly 17 applied/validated migrations.

| Run | Fresh result |
| --- | --- |
| Preparatory service-only regression | 16 tests, zero failures/errors/skips, BUILD SUCCESS. |
| Initial focused compile | BUILD FAILURE before tests: duplicate local `elapsed` in the expanded existing method. Renamed the success-case clock to `withinBudgetNanos`; no production defect or contract change. Original log retained. |
| Required focused suite after correction | **83 tests**, zero failures/errors/skips, BUILD SUCCESS, finished **2026-11-14T22:51:04+08:00**. |
| Required `-Pcontainer-smoke` focused verify | **644 Surefire + 36 Failsafe = 680 tests**, zero failures/errors/skips, BUILD SUCCESS, finished **2026-11-14T22:56:02+08:00**. Failsafe: role least-privilege **12**, reads **24**. |
| Final focused rerun after whitespace-only test cleanup | **83 tests**, zero failures/errors/skips, BUILD SUCCESS, finished **2026-11-14T22:58:46+08:00**. |

The final focused command is also repeated after whitespace-only test nesting cleanup. Its result is retained in `focused-final.log`. Counts are per invocation, not additive claims of distinct new tests.

Both required commands run from `backend` without replacing the wrapper or editing POM/CI:

```bash
JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH mvn \
  -Dmaven.repo.local=/root/.m2/repository -B -ntp \
  -DargLine=-javaagent:/root/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.15/byte-buddy-agent-1.18.15.jar \
  -Dtest=OperationsSubjectReadRequestTests,OperationsSubjectCursorCodecTests,OperationsSubjectReadServiceTests,OperationsSubjectReadConfigurationTests,OperationsSubjectReadWebTests,OperationsSubjectReadBoundaryTests,ModuleBoundaryTests,CustomerEvidenceBoundaryTests \
  test

JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH mvn \
  -Dmaven.repo.local=/root/.m2/repository -B -ntp -Pcontainer-smoke \
  -Dit.test=OperationsSubjectReadsIT,OperationsSubjectReadRoleLeastPrivilegeIT verify
```

Evidence root outside the repository: `/tmp/pa33-aggregate-fix1-20261114/`. It retains `service.log`, `focused-initial-compile-failure.log`, `focused.log`, `focused-final.log`, `container.log`, isolated `focused-xml/` and `container-xml/`, `frozen-sha256.txt`, `frozen-check.log`, `audit.py` and `final-audit.log`. The container verify also runs all 644 unit tests; the other 440 container tests are not rerun or relabeled as fresh. Existing unrelated Failsafe XML is not used as execution evidence for this fix.

The read-only audit helper required an initial missing CI dictionary binding correction and removal of an inappropriate `core.autocrlf=false` override. With the repository's existing CRLF-aware policy honored, frozen Git diff is empty and raw hashes match throughout. No frozen bytes or Git configuration are changed. Native validation logs retain ordinary Mockito/ByteBuddy dynamic-agent, JVM sharing, deprecation and annotation-processing diagnostics; these are not test failures or waived results.

## 4. Exact invariant and scope audit

- The checked-in CI inventory body is executed for its static source/POM checks; all 99 annotated classes and every exact method set are independently compared with CI. Totals remain **70/644 Surefire + 29/476 Failsafe = 99/1120**, with **1121** line-scan tokens and **29** exact POM Failsafe includes. POM and CI are unchanged.
- All eight PA33 classes retain exactly **106 identities = 70 unit + 36 container**; method names and order match accepted base `8e0ec184` exactly. Fresh relevant XML is checked against exact CI identities and zero failure/error/skip nodes, separately from static inventory.
- All **22/22 raw-byte frozen SHA256 identities** from the implementation report Appendix B pass `sha256sum -c`; frozen Git diff is empty. Exactly **V001–V017** exist. No migration, OpenAPI, docs/12/14/15/17, grant or schema change occurs.
- The five adapter diffs are exactly the wildcard-to-explicit imports. Static checks confirm the same five role names occur once in the consolidated table, service SELECTs are unchanged and all three database timeout settings are unchanged. Real role ITs pass.
- `git diff --check` passes. Changed repository paths equal the ten-path manifest below: **7 existing production + 1 existing test + 2 authorized gate artifacts**. Staged diff is empty; private `.codex` and `.pa33-plan-fix1-prompt.md` remain preserved. No new production path or annotated test identity exists.

Exact changed paths:

1. `backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/DefaultOperationsSubjectReadService.java`
2. `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsReadContext.java`
3. `backend/src/main/java/com/onecall/core/subject_number/internal/operationsread/JooqOperationsSubjectReader.java`
4. `backend/src/main/java/com/onecall/core/activation_consent/internal/operationsread/JooqOperationsActivationReader.java`
5. `backend/src/main/java/com/onecall/core/entitlement_metering/internal/operationsread/JooqOperationsTrialReader.java`
6. `backend/src/main/java/com/onecall/core/evidence/internal/operationsread/JooqOperationsEvidenceCounter.java`
7. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationsread/JooqOperationsOrderCounter.java`
8. `backend/src/test/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadServiceTests.java`
9. `docs/gateflow/pa33-aggregate-fix-1.md`
10. `docs/gateflow/pa33-operations-subject-read-foundation-implementation.md`

The implementation report update occurs only after the required focused tests pass. No web/boundary test edits are needed. No plan, Control, review artifact, fixture, POM, CI, module-policy or frozen contract path changes. Git uses the existing Windows worktree metadata through read-only `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call17` and explicit worktree; `.git` is not repaired or rewritten.

## 5. Residual owners and stop boundary

- **Backend/Platform/Database:** safe-boundary deadline observation is not asynchronous cancellation; production checkout/cleanup/driver stalls, query plans, count volumes, pool sizing, external admission and deployed latency/SLO remain deployment-validation work. No index, migration, pool or admission policy is changed here.
- **Product/Backend/Database:** existing history and candidate ceilings still fail 503 rather than fabricate partial completeness. Larger-cardinality success remains separately authorized work.
- **Identity/Platform/Security/Database:** production employee verification, six-role login provisioning, cursor-key custody/distribution/rotation and production concurrency/load evidence remain external readiness obligations. Test fixtures do not provision production identity or secrets.
- **Backend/Security:** accepted repeatable-read authority/snapshot semantics, no cross-page export guarantee and previously recorded test-coverage suggestions remain unchanged; no additional fixed test identities are authorized here.
- **Contract/OpenAPI and Security/Data:** PA33-AGG-03 remains explicitly deferred; metadata correction must not be smuggled into this fix.
- **Controller/tooling:** local focused success is not full 476-container execution, hosted CI, fresh contract/firewall/JAR-smoke runs, production acceptance or independent review acceptance. Original S1 evidence remains historical. Fresh A/B re-review is the next gate, not an action performed by this task.

No staging, commit, push, branch/PR creation or change, merge, approval, ready marking, reviewer request, external comment, issue mutation or re-review initiation is performed.

aggregate fix complete; stop for fresh A/B re-review
