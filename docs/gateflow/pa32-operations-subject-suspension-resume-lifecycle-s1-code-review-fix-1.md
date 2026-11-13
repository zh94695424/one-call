# PA32 slice 1 — bounded code-review fix 1

## Identity, authority and disposition

- Task: `pa32-s1-code-review-fix1-gpt-20261114-01`; gate: **fix**; date: 2026-11-14, Asia/Shanghai.
- Dispatch: Codex / requested GPT `gpt-6.1-sol`, reasoning `high`; underlying model identity is not independently attested by the runner.
- Read the accepted cumulative plan, PA32 Goal Confirmation and Control adjudication, S1 implementation report, validation-fix-1 and validation-fix-2, Controller validation, and independent reviews `code-review-20261114-011824.md` / `code-review-20261114-045539.md`.
- Addressed only accepted findings **PA32-CR-A-01** and **PA32-CR-A-02**. No production defect correction, diagnostic suppression change, contract change or later-gate action is included.
- **Disposition: bounded test-source fixes complete for Controller intake; new PostgreSQL runtime acceptance remains pending.** The prior Controller focused/full passes are historical evidence, not passes of this fixed candidate.

## PA32-CR-A-01 — reverse employee-revocation serialization

**Root cause:** `OperationsSubjectLifecycleIT.employeeRevocationSerializesWithLockedCurrentAuthority` exercised disable-before-Operations only. It did not establish that an Operations request which has already read current employee authority retains its locks until its owner-local transaction commits, or that subsequent replay rechecks revoked authority.

**Exact change:** extend that same existing method; its original disable-first branch remains unchanged. Re-enable the fixture employee, snapshot the owner facts, and add the reverse order using the existing jOOQ listener pattern:

1. A temporary `resultEnd` listener targets the real employee `SELECT ... FOR UPDATE`, verifies the fully fetched ACTIVE/OPS row, captures the Operations PostgreSQL PID on that same JDBC connection, signals a latch, and pauses before the service can perform owner-local writes.
2. While paused, assert unchanged owner facts and zero effects. An independent admin connection proves the subject advisory is unavailable with `pg_try_advisory_xact_lock`; activation and employee row `FOR UPDATE NOWAIT` probes each fail with `55P03`, with explicit rollback between probes. Thus the pause occurs with the subject, activation and employee locks actually held, not merely after observing SQL strings.
3. Submit disable on another identified backend, taking the prescribed administrator advisory and employee row locks before updating status/revision. Poll real `pg_locks` and `pg_blocking_pids` for that exact disable PID blocked on the Operations PID's advisory lock. Assert the disable future is unfinished, employee remains ACTIVE, and owner facts remain unchanged. The advisory gate blocks disable before it can reach its employee-row mutation; the independent NOWAIT probe separately proves that row is also locked.
4. Release Operations; require its real HTTP response to pass `assertFactSet` (including 200, revision 2→3, matching transition/Outbox/audit contents), and `oneSet()` = exactly one transition, one Outbox event and one audit fact. Await disable's update, then commit disable only after the successful Operations response/fact assertions.
5. Verify the employee is DISABLED, snapshot all owner and business facts, and replay the identical `operations-first` suspend key. Require 401 `UNAUTHENTICATED`, byte-equivalent owner/business snapshots, and the same single effect set.

Latch release and listener restoration are in `finally`; waits and future reads are bounded. No sleeps, mock authority, new endpoint, production hook or additional test method/file is introduced.

## PA32-CR-A-02 — deterministic failed-backend reuse

**Root cause:** the existing cleanup check could borrow either of the two Hikari connections, so a clean backend could mask residue on the backend whose role restoration failed.

**Exact change:** keep `OperationsLifecycleRoleLeastPrivilegeIT.failedRestorationRollsBackAndPoolReuseCannotLeakRole` and both its synthetic-collaborator and real-collaborator aborted-restoration paths. Save the original maximum/minimum pool settings; set minimum idle to zero, maximum size to one, and soft-evict existing connections before either failure. Capture the failed transaction's PostgreSQL PID before each injected division-by-zero abort (from the transaction-bound DSL identity in the synthetic path and the listener's JDBC connection in the real path). Reset the capture between paths.

Add only the non-`@Test` helper `cleanReusedBackend`. Immediately after each sanitized `SERVICE_UNAVAILABLE`, it borrows from the one-connection pool and first requires `pg_backend_pid()` to equal that specific failed PID. Only then does it assert autocommit and `current_user = session_user = fixture.login`, return the connection, and invoke the existing `cleanPool()` transaction/role/advisory-lock residue checks while the pool is still restricted to one. Each failure must still leave `allFacts()` identical to its pre-failure snapshot. A replacement/clean backend cannot satisfy the PID assertion.

The original listener providers are restored after fault injection. An outer `finally` restores the original maximum and minimum pool settings, including the fixture's normal maximum of two before later concurrency tests. Original SQL-cause preservation and sanitized failure mapping remain intact; no production behavior changes.

## Scope and preservation proof

- Exactly **two existing source files changed**: `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectLifecycleIT.java` and `backend/src/test/java/com/onecall/archtests/container/OperationsLifecycleRoleLeastPrivilegeIT.java`. `OperationsSubjectLifecycleFixture.java` is unchanged. The only new repository file is this report.
- Entry SHA256 manifest `/tmp/pa32-code-review-fix1/before.json` covers regular workspace files excluding Git metadata, preexisting `.codex`, `target` and `node_modules`. The final comparison requires exactly those two source deltas plus this new report, with no deletions. It includes existing untracked candidate sources and does not assume a clean checkout.
- Baseline copies and exact per-`@Test` block comparison prove that only the two accepted test bodies change. All other test bodies, names, ordering and annotations in both classes remain identical. The helper is not a test. There are no new, renamed, disabled, removed or skipped test methods.
- PA32 keeps **all 90 test identities**: `EmployeeSessionBoundaryTests` 8, `OperationsSubjectsWebTests` 12, `OperationsSubjectLifecycleServiceTests` 18, `OperationsLifecycleConfigurationTests` 8, `OperationsLifecycleBoundaryTests` 8, `OperationsSubjectLifecycleIT` **24**, `OperationsLifecycleRoleLeastPrivilegeIT` **12**; 54 unit + 36 integration methods. Exact identities are captured in `/tmp/pa32-code-review-fix1/pa32-test-names.json` and reconciled against unchanged CI inventories.
- Static source=CI=POM equality remains **64 classes / 574 Surefire methods + 27 classes / 440 Failsafe methods = 91 classes / 1,014 methods**, 1,015 annotation-token lines and 27 Failsafe includes. The accepted **36-path implementation allowlist** remains unchanged; this report is a gate artifact, not an added implementation path.
- All **22 plan-frozen raw-byte hashes** match, including OpenAPI, docs/12/14/15/17 and V001–V017; exactly 17 migrations, no V018. Manifest comparison additionally preserves all production files, plan/reviews/Control, prior reports, CI, POM, policy and other workspace source/configuration bytes.
- No staging, commit, branch, fetch/pull, push, PR, merge, approval, ready/reviewer request, external comment or issue mutation occurred. No re-review or later gate was entered.

## Validation performed

Evidence directory: `/tmp/pa32-code-review-fix1/`. Toolchain: `/opt/jdk21`, repository-pinned Maven wrapper 3.9.9, cached Byte Buddy agent 1.18.15. The checked-out wrapper's CRLF is removed in memory only; no wrapper/configuration file changes.

From `backend/`:

```bash
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
wrapper() { bash -c "$(sed 's/\r$//' ./mvnw)" ./mvnw "$@"; }
wrapper -B -ntp -DskipTests test-compile
wrapper -B -ntp \
  -DargLine=-javaagent:/root/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.15/byte-buddy-agent-1.18.15.jar \
  -Dtest=EmployeeSessionBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectLifecycleServiceTests,OperationsLifecycleConfigurationTests,OperationsLifecycleBoundaryTests,ModuleBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerApiBoundaryTests,CustomerSubjectActivationServiceTests test
```

| Check | Result / evidence |
| --- | --- |
| Final wrapper `test-compile` | **PASS**, exit 0; both modified ITs compile. `test-compile-final.log`. |
| Selected Surefire command | **PASS**, exit 0; **90 methods, 0 failures/errors/skips**, including all 54 PA32 unit methods. `selected-unit.log`. |
| `python3 /tmp/pa32-static.py` from repository root | **PASS**, exact source/CI/POM inventories and historical arithmetic preserved. `source-inventory.log`. |
| `python3 /tmp/pa32-code-review-fix1/check-evidence.py` from repository root | **PASS**, bounded manifest delta, unchanged other test bodies, 90 PA32 identities, all 22 frozen hashes, 17 migrations, fresh selected 90-method Surefire XML identities/counts, explicit whitespace checks. `evidence.log`. |
| Read-only `git diff --check` | **PASS**, exit 0; known CRLF warnings only. Explicit source/report whitespace checks also cover untracked candidate files. `git-diff-check.log`. |
| `docker info` | **BLOCKED**, exit 1: permission denied at `unix:///var/run/docker.sock`. `docker-info.log`. |

Git inspection uses `GIT_OPTIONAL_LOCKS=0`, `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call16`, `GIT_WORK_TREE=$PWD` and `-c core.fsmonitor=false` without changing the Windows `.git` pointer or metadata. No container skip switch is used and no Failsafe success is claimed.

## Limitations, residual risk and Controller intake

The selected 90-unit run is **not** the accepted focused 78-unit / 70-integration command or the full clean suite: it omits listening-server `CustomerWebConfigurationTests` and includes 18 customer activation service units instead. Docker access remains denied. Therefore the new PostgreSQL lock probes, latch interleaving, failed-backend PID equality and pool-residue assertions are compiled but **not locally runtime-verified**; this is the remaining test-validation risk. Existing Controller integration reports are not overwritten by an attempted Failsafe run.

Controller must rerun the accepted focused PostgreSQL16 command (**78/78 Surefire + 70/70 Failsafe, zero skips**) and full clean acceptance (**574/574 + 440/440**, exact XML identities), paying particular attention to both unchanged method names above and normal two-connection concurrency after restoration. Prior passing evidence cannot substitute for those fixed-candidate runs. Production verifier/provisioning/UTF8 deployment and other previously assigned external risks remain unchanged; no production-readiness claim is made.

**Stop for Controller intake.** No further gate, review dispatch or remote action is authorized or performed by this fix.
