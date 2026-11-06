# PA15 Public Share Read — Code Review Fix 1

- Task: `pa15-public-share-read-code-review-fix1-gpt-20261106`.
- Work unit: `PA15-sharing-public-share-read`; gate: `fix`; author: GPT.
- Date: 2026-11-06, Asia/Shanghai (UTC+08:00).
- Status: **fix-complete; required local validation passed**. No gate advancement, acceptance or re-review is claimed or initiated.

## 1. Authority And Accepted Findings

The active Control adjudication in `docs/gateflow/project-execution-control.md:67` accepts only Reviewer A's F-PA15-02 and F-PA15-03 as test-evidence defects, rejects F-PA15-01, and authorizes the three test paths below plus this artifact. Both reviews were read: `docs/reviews/code-review-20261106-074823.md` and `docs/reviews/code-review-20261106-075249.md`. Reviewer B has no material finding. The accepted plan, implementation report and current candidate were inspected; no production defect was found or production behavior changed.

Accepted authority identities remain byte-for-byte unchanged:

| Authority | SHA-256 |
| --- | --- |
| `docs/gateflow/pa15-sharing-public-share-read-plan.md` | `eed82e23743c07b71afae09c5183757dbafdfc44e3b71fea55cf67c2518a8012` |
| `docs/gateflow/pa15-sharing-public-share-read-implementation.md` | `f2cd8dcb71029490807cd59a01414aa2dfbbfaa76d16e20c2735d0e2ccbced82` |

### F-PA15-02 — Matching Malformed Capture Timestamp

Before: `immutableEvidenceProjectionIsOrderedAndFailClosed` changed only the frozen snapshot's `capturedAt` from `2026-11-05T23:58:20Z` to offset-less `2026-11-05T23:58:20`. The evidence projection retained the valid timestamp, so identity equality failed before timestamp parsing.

After: inside that same method, a separate valid NONE-mode fixture supplies the identical malformed identity to both the frozen grant snapshot and a SOLIDIFIED evidence projection. An explicit JSON equality assertion proves that the identity comparison is satisfied. All other projection fields and artifact tuples come from the valid fixture. The service must then reject the offset-less value through its timestamp parser with `SERVICE_UNAVAILABLE`. The test also verifies that the reader was invoked and the sharing role restored.

The old snapshot-only timestamp case is replaced by this parser-specific case, not counted as an additional test method. Existing null reader results, throwing reader, malformed snapshots/artifacts, non-SOLIDIFIED evidence, absent duration and role-restore failure assertions remain. The integration suite's extra-artifact assertion is unchanged.

### F-PA15-03 — Explicit Unused-Provider Counters

Before: the fixture counted only creation-policy calls among the unused command providers. The repeated-read test also cleared observations between CLEAR/prefetch reads and OPEN-risk reads, which would discard first-phase calls.

After: `PublicSharesFixture` adds `tokenIssuances`, `extractionCodes`, `designatedMobiles` and `rotationPolicies`, each an explicit `AtomicInteger`. Each registered deterministic provider increments its counter before delegating to the original implementation. Real seed creation therefore traverses the instrumented issuance/code/mobile providers when applicable; seeding does not call rotation policy. The existing post-seed `clearObservations()` resets all four counters plus the retained creation-policy counter.

Inside the existing `repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged`, all five counters must remain zero. The midway reset is removed, so those assertions cover the complete repeated/prefetch/CLEAR/OPEN observation window, across all verification modes and both anonymous/member sessions. Existing response-envelope equality, no-risk/no-mutation SQL and eleven-table digest equality assertions remain.

### F-PA15-03 — Direct Null Evidence Reader

Before: `service(..., null)` in the integration helper selected the configured healthy reader, so it could not test a missing reader. Missing/Unavailable reader results did not cover a null dependency.

After: inside the existing `publicProjectionAndDependencyFailuresFailClosedWithoutPartialContent`, a healthy HTTP read first confirms 200 for the seeded NONE-mode token/content path. A direct `PublicShareQueryService` construction then supplies the real transaction template, DSL and repository, a deterministic SHA provider, and an explicit null `ShareableEvidenceProjectionReader`. The existing denial helper requires the operation to throw `SERVICE_UNAVAILABLE`, with the exact sanitized message and no cause; returning any complete or partial view cannot pass. The eleven-table snapshot is immediately asserted unchanged. No helper overload or new test method/class is added; all subsequent dependency/projection assertions remain.

### Rejected F-PA15-01 — Deliberately Unchanged

The original line 174 already calls `denied(fixture::read, SERVICE_UNAVAILABLE)` immediately after installing the throwing, non-null membership callback, while the fixture still has its valid tenant-bearing grant. The later null-callback call is a separate probe. The Controller rejected this finding for that reason. `organizationContentRequiresCurrentMatchingMember` is byte-for-byte unchanged against the SHA-256-verified pre-fix freeze; only its line numbers move because of the earlier timestamp block.

## 2. Exact Changed Paths

Only these existing source/test files change relative to the pre-fix candidate:

1. `backend/src/test/java/com/onecall/archtests/sharing/PublicShareQueryServiceTests.java` — matching malformed timestamp and parser-specific fail-closed assertions, inside the existing method.
2. `backend/src/test/java/com/onecall/archtests/container/PublicSharesFixture.java` — four provider counters, deterministic delegating wrappers and observation resets.
3. `backend/src/test/java/com/onecall/archtests/container/PublicSharesIT.java` — direct null-reader case and continuous five-counter zero-call assertions, inside the two existing methods.

The only new repository file is `docs/gateflow/pa15-sharing-public-share-read-code-review-fix-1.md`, this report. No production/main/config/schema/OpenAPI/plan/Control/review/CI/POM/README path changes in this fix. No test method or class is added or removed.

## 3. Validation Commands And Results

Commands ran from `backend` with Java 21.0.13 and Maven wrapper 3.9.9. The checked-in CRLF wrapper was evaluated in memory; its bytes were not edited. Container runs used the local Docker socket, Docker server 29.1.3 and actual disposable `postgres:16` containers. Both targeted and fresh full runs report PostgreSQL **16.15**.

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:$PATH
export DOCKER_HOST=unix:///var/run/docker.sock
export TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1
set -o pipefail
sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -B -ntp \
  -Dtest=PublicShareWebTests,PublicShareQueryServiceTests,PublicSharePersistenceConfigurationTests,CustomerApiBoundaryTests,SharingQueryBoundaryTests,ModuleBoundaryTests test \
  2>&1 | tee /tmp/pa15-code-review-fix-1/focused.log
sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -B -ntp \
  -Pcontainer-smoke -Dit.test=PublicSharesIT verify \
  2>&1 | tee /tmp/pa15-code-review-fix-1/targeted.log
sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -B -ntp \
  clean -Pcontainer-smoke verify \
  2>&1 | tee /tmp/pa15-code-review-fix-1/clean-verify.log
python3 /tmp/pa15-code-review-fix-1/ci-inventory-check.py
```

| Run | Exact result | Completion (UTC+08:00) |
| --- | --- | --- |
| Focused Surefire | 6 classes / 24 methods, including all 4 `PublicShareQueryServiceTests`; zero failures/errors/skips; BUILD SUCCESS | 2026-11-06 08:06:17 |
| Targeted container verify | Surefire 19/91 and targeted `PublicSharesIT` 1/8; zero failures/errors/skips; BUILD SUCCESS | 2026-11-06 08:08:15 |
| Fresh full clean verify | Surefire **19/91** plus Failsafe **7/126**, combined **217**; zero failures/errors/skips; BUILD SUCCESS | 2026-11-06 08:11:50 |
| Exact inventory checker | Unchanged CI Python body extracted verbatim and run locally against source and fresh clean XML: **source=CI=XML 19/91 + 7/126 = 217** | After fresh full completion |

Focused arithmetic is web 6 + service 4 + persistence configuration 2 + customer API architecture 3 + sharing architecture 1 + module architecture 8 = 24. Full Failsafe arithmetic remains PostgresContainerIT 64 + SharingQueryServiceIT 10 + CustomerMeIT 10 + CustomerSharesIT 8 + SharingRoleLeastPrivilegeIT 8 + CustomerShareCommandsIT 18 + PublicSharesIT 8 = 126. These overlapping runs are not additional methods. The exact CI checker verifies class and per-method identity, source-class equality, duplicates/unsupported annotations and zero non-green XML results, not merely aggregate counts.

Both container runs retain the eleven-table no-write success line across repeats/prefetch/CLEAR/OPEN. The full run recreates `target` via Maven clean; targeted stale XML cannot satisfy its inventory. Clean XML independently reproduces 19/91 and 7/126, and both focused and targeted XML were archived before being replaced.

An initial focused attempt failed at test compilation because the new extraction-code wrapper used `issue` instead of the existing `generate` interface method. That test-only typo was corrected before the successful runs above. `/tmp/pa15-code-review-fix-1/focused-initial-compile-failure.log` is retained as failed diagnostic evidence, not represented as a pass. Existing compiler/Mockito warnings and intentional negative migration diagnostics do not represent failed/skipped tests; fresh XML is entirely green.

## 4. Retained Evidence And Scope Audit

Evidence root: `/tmp/pa15-code-review-fix-1/`. The original candidate's older `/tmp/pa15-*.log` evidence is not overwritten.

| Evidence | Purpose / SHA-256 where shown |
| --- | --- |
| `focused.log` | Successful focused run; `818acb99f568295076d8c29fa1da310a2ba429bcd8c03d673e6dc89900b100fe` |
| `targeted.log` | PostgreSQL 16.15 targeted verify; `05bb2c26c849ab3acafea686e2af4f19d4cb86ebe056e8af8daedfa51eaf5fbc` |
| `clean-verify.log` | Fresh complete clean verify; `6471cb7742817e8bfa28fa8b342a5d2a9e21b0b42b1e6f7b9adfae672f6cfce6` |
| `inventory.log` | Exact source=CI=XML checker success; `6806bc4d32964d476ab784f3805871ba508872cb2337c96e48777ee772850c7f` |
| `ci-inventory-check.py` | Verbatim 337-line CI inventory-check body, executed from `backend` |
| `focused-reports/`, `targeted-reports/`, `clean-reports/` | Retained focused six-suite XML, targeted PublicSharesIT XML/summary, and complete clean Surefire/Failsafe reports |
| `source-counts.log`, `prior-run-xml-summary.log`, `clean-xml-summary.log` | Independent source and XML counts/results |
| `baseline-hashes.json`, `pre-artifact-audit.log`, `final-audit.log` | Pre-fix SHA-256 manifest, exact-path comparisons and protected authority hashes |
| `test-diff.log` | Complete before/after test diffs using freeze files first verified against pre-fix SHA-256; rejected F1 method unchanged |
| `diff-check.log`, `hashes-before-artifact.log`, `evidence-sha256.log`, `toolchain.log` | Read-only Git whitespace/staging audit, authority/test/log hashes, retained evidence hashes and toolchain identity |

The pre-fix manifest covers 1,386 repository files, excluding Git metadata, private `.codex`, generated `target` directories and `node_modules`. The exact delta is the three allowed test files plus this new artifact; **all other 1,383 baseline files are byte-identical**, including every other candidate path, the accepted plan and implementation report. Read-only Git checks use the confirmed live directory `/mnt/d/codex-call/.git/worktrees/codex-call15`, with process-local `GIT_DIR`, `GIT_WORK_TREE`, `GIT_OPTIONAL_LOCKS=0` and `GIT_NO_LAZY_FETCH=1`; `.git` is not rewritten. `git diff --check` passes (checkout line-ending warnings only); the staged diff remains empty. A direct whitespace audit also covers the three untracked candidate tests and this artifact, which ordinary tracked Git diff does not cover.

## 5. Residual Risks And Action Boundary

- No blocker remains for completing this bounded local fix. Acceptance, any re-review dispatch and later gates remain Controller-owned and are not entered here.
- Local PostgreSQL/HTTP tests do not establish production customer-session verification, provisioning, load/abuse controls, ingress behavior or a dedicated public-read ACL. Existing accepted operational risks remain unchanged.
- Packaged-JAR smokes and the 82-probe dependency firewall were not rerun for this test-only fix; their earlier candidate evidence is preserved, not represented as new execution. No production dependency/configuration change is made.
- `/tmp` evidence is local and replaceable. Controller/runner must retain it with the candidate; clean XML is additionally copied outside `target`. No hosted CI or remote acceptance is claimed.
- No production system or production source was changed. No staging, commit, push, branch/worktree creation, PR creation, merge, approval, ready action, reviewer request, external comment, issue mutation or other remote mutation was performed. Git use was read-only local inspection only.
