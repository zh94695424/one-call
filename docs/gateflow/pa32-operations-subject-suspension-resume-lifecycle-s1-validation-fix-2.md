# PA32 slice 1 — bounded validation fix 2

- Task: `pa32-s1-fix2-gpt-20261113-01`; gate: **fix**.
- Date: 2026-11-14, Asia/Shanghai.
- Scope: one expected failure-channel literal in the existing race test and this new evidence artifact. Stop for Controller focused rerun; no later gate or production-readiness claim.

## Root cause and exact correction

Controller reported the accepted focused PostgreSQL 16.15 rerun after validation fix-1: Surefire **78/78**, Failsafe **69/70**, zero skips. The remaining failure was `OperationsSubjectLifecycleIT.activationAndCustomerCommandRacePreservesOneAuthoritativeHead`: expected `CONSENT_REQUIRED`, actual `FORBIDDEN`.

Before editing, read the accepted PA32 plan, its fix artifacts and binding Control checkpoint, PA31 final closeout/accepted suspended-subject semantics, the current race test and `DefaultCustomerSubjectActivationService`. PA31's accepted `PA31-PLAN-REV-01` explicitly requires a fresh ACCEPT on a currently SUSPENDED subject to return **403 FORBIDDEN**, not **409 CONSENT_REQUIRED**, with no reactivation. The service already implements that refusal after taking the shared subject advisory lock and reading the authoritative head; no production correction is needed.

The test releases two waiters on that same subject lock and requires the Operations suspension to return 200. If the customer executes first, its activation result remains permitted. If Operations executes first, the fresh customer ACCEPT sees SUSPENDED and must fail with `SubjectActivationException.Code.FORBIDDEN`. The old assertion contradicted that frozen behavior and made success depend on lock acquisition order.

Changed only `SubjectActivationException.Code.CONSENT_REQUIRED` to `SubjectActivationException.Code.FORBIDDEN` in that existing method's failure branch. The successful `ActivationResult` branch, Operations 200 assertion, barrier/waiter synchronization, timeouts, final single SUSPENDED revision-3 head assertion and `oneSet()` remain byte-identical. No other assertion clarification was necessary.

## No-production and no-arithmetic proof

- Entry manifest `/tmp/pa32-s1-fix2/before-pruned.json` captures **1,995 regular working files**, excluding Git metadata, preexisting `.codex`, build output and `node_modules`. The final comparison requires exactly the one existing test-file change plus this new report, with no deleted file. This covers preexisting untracked implementation files as well as tracked files; the workspace is not claimed clean.
- An exact byte comparison against `/tmp/pa32-s1-fix2/race-baseline.java` requires the entire test file to equal its original bytes with exactly that one enum literal replaced. Consequently every other method, test name, annotation, method ordering and test count is unchanged; no test is added, removed, renamed, disabled or skipped.
- All production paths, frozen contracts, OpenAPI, migrations, policy/security validators, Control, accepted plan/fixes/reviews, prior implementation/validation reports, CI and POM remain byte-identical to task entry. All **22 accepted frozen raw hashes** still match plan §11; exactly **V001–V017**, no V018.
- Static source/CI/POM inventory remains **64 classes / 574 Surefire methods + 27 classes / 440 Failsafe methods = 91 classes / 1,014 methods**, with **1,015 annotation-token lines** and **27 Failsafe includes**. PA32 still adds the same 54 unit / 36 integration methods; its 36-path implementation allowlist is unchanged. The new report is a gate artifact, not a new implementation path.
- No production behavior, lock order, role handoff, transition/replay behavior, trial issuance, bounds, error contract or arithmetic changes. No staging, commit, branch, push, PR, merge, approval, ready/reviewer request or external comment occurred.

## Validation

Evidence: `/tmp/pa32-s1-fix2/`. Toolchain: `/opt/jdk21`, repository-pinned Maven **3.9.9** wrapper, existing cached Byte Buddy agent **1.18.15**. The wrapper's checked-out CRLF is removed in memory only; no wrapper or build configuration file is changed.

From `backend/`:

```bash
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
wrapper() { bash -c "$(sed 's/\r$//' ./mvnw)" ./mvnw "$@"; }
agent=/root/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.15/byte-buddy-agent-1.18.15.jar
wrapper -B -ntp -DskipTests test-compile
wrapper -B -ntp -DargLine=-javaagent:"$agent" \
  -Dtest=EmployeeSessionBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectLifecycleServiceTests,OperationsLifecycleConfigurationTests,OperationsLifecycleBoundaryTests,ModuleBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerApiBoundaryTests,CustomerSubjectActivationServiceTests test
```

| Check | Result |
| --- | --- |
| Wrapper `test-compile` | **PASS**, exit 0; modified IT compiles. `test-compile.log`. |
| Selected Surefire command | **PASS**, exit 0; **90 tests, zero failures/errors/skips**, including all 54 PA32 unit methods and 18 customer activation service methods. `selected-unit.log`. |
| `python3 /tmp/pa32-static.py` from repository root | **PASS**; unchanged exact source=CI=POM inventory, historical arithmetic and includes. `source-inventory.log`. No full-suite XML claim. |
| `python3 /tmp/pa32-s1-fix2/check-evidence.py` from repository root | **PASS**; bounded hash delta, exact one-literal byte substitution, all 22 frozen hashes, 17 migrations and fresh selected 90-method Surefire XML identities/counts with zero failures/errors/skips. `evidence.log`. |
| `git diff --check` and explicit untracked-output whitespace checks | **PASS**; no whitespace errors. `git-diff-check.log`, `untracked-whitespace.log`. |
| `docker info` | **BLOCKED**, exit 1; permission denied at `unix:///var/run/docker.sock`. `docker-info.log`. No skip switch or integration-test success claimed. |

Git checks use `GIT_OPTIONAL_LOCKS=0`, `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call16`, `GIT_WORK_TREE=$PWD` and `-c core.fsmonitor=false`, without editing the checkout's Windows `.git` pointer. This explicit mapping is needed because plain Git discovery cannot resolve that pointer in WSL. Read-only staged-diff inspection is empty.

## Limitations and Controller handoff

The local 90-method unit selection is not the accepted 78-unit / 70-integration focused command: it omits the listening-server `CustomerWebConfigurationTests` and adds the 18 customer activation service units. Docker access is denied, so the changed race method, PostgreSQL locking/rollback, full focused Failsafe rerun, full-suite XML equality and packaged/listening checks are not locally verified. Existing Controller integration reports are preserved; no new Failsafe run overwrites them.

Controller must rerun the exact accepted focused suite on PostgreSQL 16.15, requiring **78/78 Surefire and 70/70 Failsafe, zero skips**, with the existing race name and both serialized outcomes preserved. No additional implementation or review is entered. **Bounded fix complete; stop for Controller focused rerun.**
