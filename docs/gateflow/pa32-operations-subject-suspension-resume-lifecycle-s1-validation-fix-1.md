# PA32 slice 1 — bounded validation fix 1

## Identity and status

- Task: `pa32-s1-fix1-gpt-20261113-01`; gate: **fix**, for Controller intake only.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa32-next-work-unit/codex-call`.
- Task date: 2026-11-13; local validation crossed midnight into 2026-11-14, Asia/Shanghai. Compile completed at `2026-11-13T23:59:25+08:00`; selected units completed at `2026-11-14T00:00:31+08:00`.
- Read the accepted cumulative plan, current implementation report, implementation and focused failures. The bounded delta is **two existing allowlisted test/fixture files plus this one new report**. No production implementation change is necessary: both focused failures originate in the PA32 fixture, not in the frozen service bounds or typed error policy.
- **Local fix and available checks complete; PostgreSQL16 focused acceptance remains pending Controller rerun.** No integration success, review, later gate or closeout is claimed.

## Failure 1 — aborted restoration incorrectly surfaced as INTERNAL

The injected collaborator in unchanged `OperationsLifecycleRoleLeastPrivilegeIT.java:58` switches to `operations_iam_role`, executes `SELECT 1/0`, and tries to restore `activation_consent_role` in `finally`. The fixture registers an `executeStart` listener that queries current role, login, backend PID and transaction identity **before each lifecycle statement**, including the restoration statement.

After the division error aborts the transaction, that instrumentation query also fails, with SQLSTATE `25P02`. Previously `OperationsSubjectLifecycleFixture` caught the `SQLException` and threw `new IllegalStateException("Fixture statement capture failed")`, discarding its cause. This replaced the technical restoration failure with a cause-free programming exception. The existing service mapper at `DefaultOperationsSubjectLifecycleService.java:88` cannot identify a discarded SQL cause and correctly reserves INTERNAL for unknown nontechnical defects. Broadly mapping every `IllegalStateException` to 503 would weaken that distinction and break the existing unknown-defect contract.

**Fix:** retain the original `SQLException` as the instrumentation exception's cause. The unchanged owner transaction marks rollback-only, rolls back, and only then the unchanged mapper traverses the cause chain and emits a sanitized lifecycle `SERVICE_UNAVAILABLE`. No raw SQL cause is attached to the public lifecycle failure. The real production employee collaborator's typed failure/restoration handling is unchanged; the focused IT's second real-collaborator failure injection is also unchanged.

Existing unit coverage now additionally injects this precise wrapper shape from the employee collaborator for both division-by-zero (`22012`) and aborted transaction (`25P02`), and requires 503's typed code, rollback, no commit, no writes, cleared transaction activity and a cause-free public failure. The original cause-free unknown-defect expectation remains INTERNAL. The failing IT name, assertion of `SERVICE_UNAVAILABLE`, original fault injection, pool-reuse checks and fact comparisons are untouched.

## Failure 2 — maximum Unicode reason rejected by fixture database encoding

PA32 composes `CustomerSubjectActivationFixture`, which composes `CustomerBusinessNumbersFixture`. The inherited PostgreSQL16 container arguments at `CustomerBusinessNumbersFixture.java:34` set `--lc-collate=C --lc-ctype=C` but do not select an encoding. With libc C locale, initdb defaults to SQL_ASCII, not UTF8. This is a server/database encoding issue, not the Java source encoding, JDBC client's UTF8 encoding or an action-key truncation.

Both the checked-in IT reason `"原因😀".repeat(21) + "x"` and the Controller's exact reason `"原因😊".repeat(21) + "x"` contain **64 Unicode code points and 211 UTF8 bytes**. The Java binder and owner correctly accept these strings using code-point counts. The 128-character ASCII action key also satisfies its bound. However, SQL_ASCII does not interpret those UTF8 bytes as multibyte characters: PostgreSQL `length(reason_code)` sees 211. The first incompatible frozen database check is the transition INSERT's `ck_activation_transition_reason`, `V007__activation_and_consent.sql:157`, which requires length 1–64. That check violation aborts the transaction and the existing technical-error mapping returns 503. Audit and Outbox are downstream of that INSERT; they are not a reason to reduce the accepted input or truncate it. The audit's own text bound would also not make SQL_ASCII suitable for this input.

**Fix:** override `POSTGRES_INITDB_ARGS` only on this PA32-owned composed fixture before container startup, explicitly selecting `--encoding=UTF8` while retaining both C-locale arguments. Assert `SHOW server_encoding = UTF8` immediately after connecting, alongside the existing PostgreSQL16 version assertion. The inherited customer fixture file is outside this bounded write set and is not modified. No migration, check constraint, SQL binding, application validation, fingerprint or exception mapping changes are made.

The common fact assertion additionally compares the persisted transition's `reason_code` to the response reason; its existing exact key, fingerprint, audit reason/snapshot and Outbox assertions remain. The existing maximum-action unit method now also exercises both the original IT emoji and the Controller's exact emoji, with the exact 128-character ASCII key, 64-code-point reason and expected revision 2. It asserts exact stored action/reason, exact returned reason, identical replay and one transition/Outbox/audit. The existing HTTP maximum-input IT and its 200 expectation remain byte-for-byte unchanged.

These database and instrumentation paths are source-traced diagnoses. This sandbox cannot recapture the original JDBC constraint failure or execute the repaired restoration/pool checks against PostgreSQL; the Controller must confirm them with the unchanged focused suite. The new server-encoding assertion makes the database precondition explicit rather than silently depending on the image locale.

## Exact changed locations

Paths below are relative to `backend/src/test/java/com/onecall/archtests/`:

| File and final line | Change and purpose |
| --- | --- |
| `container/OperationsSubjectLifecycleFixture.java:61` | Select UTF8 before starting the inherited container; retain C collation/ctype. |
| `container/OperationsSubjectLifecycleFixture.java:65` | Assert the actual database server encoding is UTF8. |
| `container/OperationsSubjectLifecycleFixture.java:86` | Preserve the caught SQL cause in statement instrumentation so the existing post-rollback mapper can identify a technical failure. |
| `container/OperationsSubjectLifecycleFixture.java:153` | Strengthen persisted transition reason equality against the required response field. |
| `activation/OperationsSubjectLifecycleServiceTests.java:122` | Extend `maximumActionIdentityIsStoredWithoutTruncation` with both maximum Unicode reasons, exact Controller key/revision, persisted/returned text, replay and one-fact checks. |
| `activation/OperationsSubjectLifecycleServiceTests.java:184` | Extend `uniqueAndTechnicalFailuresMapOnlyAfterTransactionCleanup` with wrapped SQLSTATE 22012/25P02 from the employee handoff and cleanup/sanitization assertions. |

The only added repository path is this report, `docs/gateflow/pa32-operations-subject-suspension-resume-lifecycle-s1-validation-fix-1.md:1`.

## No weakening or scope drift

- Entry snapshot `/tmp/pa32-s1-fix1/before.json` records 1,977 working-file hashes and all 127 test-source method inventories. Post-fix comparison identifies exactly the two allowlisted files above; existing production, accepted plan/fixes/reviews, implementation report, Control, policy/security validator, CI and POM remain unchanged from task entry. Preexisting `.codex` was neither read nor modified.
- Every captured test-method list remains identical. All 90 accepted PA32 test names and ordering still match plan §8: `[8,12,18,8,8,24,12]`, or 54 unit / 36 integration methods. No test method/class is added, removed, renamed, disabled or skipped. Both Controller-focused IT files are unchanged.
- Static extraction of the unchanged CI source inventories and POM includes passes: **64 classes / 574 unit methods + 27 classes / 440 integration methods = 91 / 1,014**, with exactly 27 Failsafe includes. This is source/CI/POM evidence, not a claim of full XML acceptance.
- The implementation allowlist remains exactly **36 paths**. All 22 accepted frozen raw SHA-256 values match plan §11; migration inventory remains exactly V001–V017, with no V018. No protected contract/document bytes were normalized.
- The 1–128 action and 1–64 reason bounds, nonblank checks, exact HTTP statuses/error set, employee authority/audience, same-resource transaction/role handoff and restoration, lock order, replay/conflict/fingerprint identities and no-trial-on-resume behavior are unchanged. All production files are byte-identical to task entry.
- No staging, commit, branch, fetch/pull/push, PR/issue operation, approval, review or external comment was performed.

## Validation commands and results

Evidence directory: `/tmp/pa32-s1-fix1/`. Toolchain: Linux `/opt/jdk21`, Temurin **21.0.13+9**; repository wrapper **Maven 3.9.9**; Byte Buddy agent **1.18.15** from the existing local dependency cache.

Direct `./mvnw -B -ntp -DskipTests test-compile` failed before Maven, exit 126: checked-out wrapper has a CRLF shebang (`/bin/sh^M`). Log: `test-compile.log`. As in the accepted implementation report, use the same pinned wrapper with CR removed **in memory only**, not by changing the wrapper file. From `backend/`:

```bash
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
wrapper() { bash -c "$(sed 's/\r$//' ./mvnw)" ./mvnw "$@"; }
agent=/root/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.15/byte-buddy-agent-1.18.15.jar

wrapper -B -ntp -DskipTests test-compile
wrapper -B -ntp -DargLine=-javaagent:"$agent" \
  -Dtest=EmployeeSessionBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectLifecycleServiceTests,OperationsLifecycleConfigurationTests,OperationsLifecycleBoundaryTests,ModuleBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerApiBoundaryTests test
wrapper -B -ntp -DargLine=-javaagent:"$agent" -Pcontainer-smoke \
  -Dit.test=OperationsSubjectLifecycleIT,OperationsLifecycleRoleLeastPrivilegeIT,CustomerSubjectActivationsIT,ActivationCommandRoleLeastPrivilegeIT \
  failsafe:integration-test failsafe:verify
```

| Check | Result |
| --- | --- |
| Pinned wrapper `test-compile` | **PASS**, exit 0; production/test compile including both modified files. `test-compile-wrapper.log`. |
| Selected Surefire units | **PASS**, exit 0; **72 tests, 0 failures, 0 errors, 0 skipped**, including all 54 PA32 unit methods and strengthened regression cases. `selected-unit.log`; current XML method identities/counts independently checked. |
| `docker info` | **BLOCKED**, exit 1; permission denied connecting to `unix:///var/run/docker.sock`. `/tmp/pa32-fix1-docker.log`. |
| Four-class Failsafe diagnostic above | **BLOCKED/FAILED**, exit 1; **4 startup errors, 0 failures, 0 skipped**, Testcontainers cannot find a valid Docker environment. No lifecycle test body reaches PostgreSQL. `container-attempt.log`. No no-Docker skip switch used. |
| `python3 /tmp/pa32-static.py` from repository root | **PASS**, exit 0; unchanged exact source=CI=POM 64/574 + 27/440 inventory, historical assertions and 27 includes. `source-inventory.log`. |
| `python3 /tmp/pa32-s1-fix1/check-evidence.py` from repository root | **PASS**, exit 0; bounded hash delta, accepted 36 paths, all test-name lists, all 22 frozen hashes, 17 migrations, both maximum Unicode lengths and selected 72-method XML. `evidence.log`. |
| Read-only Git hygiene / `git diff --check` | **PASS**, exit 0; no whitespace errors; no staged files. `git-status.log`, `git-diff-paths.log`, `git-untracked.log`, `git-staged.log`, `git-diff-check.log`. |

Git commands use `GIT_OPTIONAL_LOCKS=0`, `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call16`, `GIT_WORK_TREE=$PWD`, and `-c core.fsmonitor=false`, preserving the checkout's Windows gitdir file unchanged. Original Controller Failsafe XML was copied to `controller-failsafe-before/` before the local blocked diagnostic overwrote build-output reports.

The 72-test command deliberately omits the listening-server `CustomerWebConfigurationTests` class in this socket-restricted sandbox, as the prior implementation report did. It is not the accepted 78-unit / 70-integration focused command and does not replace it. Full focused PostgreSQL16 verification, full clean verification, complete XML equality and packaged/listening probes are not claimed or advanced by this bounded fix. No unrelated validators or frozen files were repaired.

## Residual risks and handoff

1. **Controller, blocking acceptance:** rerun the exact accepted focused PostgreSQL16 command with the pinned agent. Require both original failing IT assertions to pass, including rollback, role restoration, pool reuse, one fact set and maximum-text round trip; inspect the explicit UTF8 server assertion. Docker denial prevents runtime confirmation here.
2. **Database fixture scope:** UTF8 initialization is intentionally limited to PA32's newly owned composed fixture. The historical customer fixture remains unchanged; changing its shared bootstrap is outside this bounded fix.
3. **Security:** retaining an internal SQL cause is for correct technical classification only. Existing owner sanitization still emits a cause-free typed failure; the selected unit tests explicitly assert that. Unknown programming defects continue to map to INTERNAL, not blanket 503.
4. **Completion:** the requested bounded fix, unchanged inventory/bounds proof and local checks are complete. Stop for Controller intake; no code review/deepreview/PR/final-closeout is entered.
