# PA40A S1 — Implementation Fix 3

- Task label: `pa40a-customer-catalog-offer-read-acl-s1-fix3-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Workdir: `/mnt/c/Users/Administrator/.codex/worktrees/pa40a-customer-catalog-offer-read-acl/codex-call`.
- Scope: two historical PA25 test-literal corrections and implementation evidence only; no staging, commit, push, PR, merge, or review entry.

## 1. Controller Evidence And Root Causes

Controller reports that the first regression-IT selector ran 140 tests: 138 passed, one failed, and one errored. This is Controller-provided runtime evidence, not a local WSL rerun.

- `CustomerBusinessNumberReadPrivilegeIT.pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle`: current V019 clean ACLs total 312; the intentional `subject_number.caller_number_binding.number_hmac` grant adds one tuple, so the widened state must contain exactly 313 rather than the historical 291. The local method name lacks the `WithoutRepair` suffix used in the task description; the affected assertion is unambiguous and the method is not renamed.
- `CustomerSubjectDetailReadPrivilegeIT.pa25RequiresExactV016HistoryAndIndependent269TupleOracleWithoutRepair`: moving the V016 row to installed rank 19 collides with legitimate V019 and fails with a duplicate-key SQL error before PA25 evaluates the mutation. Rank 20 remains invalid for V016 but does not collide with current V001–V019 history.

## 2. Exact Changes And Preserved Guards

- `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java:265`: change only widened-state `hasSize(291)` to `hasSize(313)`.
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java:559`: change only the hostile mutation literal `installed_rank=19` to `installed_rank=20`.
- `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-2.md`: append Controller continuation without rewriting existing evidence.
- `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-3.md`: this report.

Byte comparisons against pre-edit snapshots confirm exactly one literal replacement per Java file, with every other byte, including mixed line endings, preserved. The existing clean current 312 assertion, widened superset and exact intentional-tuple checks, catalog comparisons, negative PA25 apply/check checks, history mutation cases, and restoration logic remain unchanged. No guard is removed or weakened. The fix-2 report's original bytes remain an exact prefix.

No production, migration, PA25 implementation, other test, Control, plan/review, POM/CI, or OpenAPI file is changed by this fix. Existing unrelated worktree changes remain intact. Maven writes normal ignored build outputs under `backend/target`.

## 3. Native Validation

Run from `backend` using `JAVA_HOME=/opt/jdk21` and `PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH`: WSL-native Temurin OpenJDK 21.0.13 and Maven 3.9.9, offline cached dependencies. No Windows executable is used for these validation commands.

```bash
mvn -o -B -ntp -Pcontainer-smoke -DskipTests test-compile
mvn -o -B -ntp -Dtest=CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT -DskipTests test-compile
```

| Validation | Exit status | Result |
| --- | --- | --- |
| Container-smoke profile test compilation | 0 | BUILD SUCCESS; 298 production and 192 test source files compiled |
| Two-class selector test compilation | 0 | BUILD SUCCESS; compiled classes already up to date |

These are compilation checks only: `-DskipTests` and the `test-compile` phase do not execute either IT class. The first build emits compiler warnings for missing JAXB enum metadata, deprecated APIs, and unchecked test operations; none prevents successful compilation.

Scoped whitespace validation uses the existing worktree metadata read-only because the inherited `.git` pointer contains a Windows drive path:

```bash
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call10 GIT_WORK_TREE="$PWD" git diff --check -- backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-2.md docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-3.md
```

The new untracked report is also checked with `git diff --no-index --check -- /dev/null docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-3.md`, and the existing untracked fix-2 report is checked the same way, so both documents are covered without staging.

Scoped `git diff --check` exits **0** with no whitespace errors. Each supplemental `--no-index --check` exits **1** for new-file differences from `/dev/null`, with no whitespace-error diagnostics. Git emits LF-to-CRLF advisory warnings only; the commands do not rewrite files.

Native logs: `/tmp/pa40a-fix3-container-smoke-test-compile.log` and `/tmp/pa40a-fix3-two-class-test-compile.log`. Logs and generated build artifacts are not added to the patch.

## 4. Handoff Boundary

WSL cannot execute Docker here. Controller must rerun `CustomerBusinessNumberReadPrivilegeIT` and `CustomerSubjectDetailReadPrivilegeIT` through Failsafe on Windows/PostgreSQL16. No local Docker, PostgreSQL runtime IT, Failsafe, full verify, or review gate is launched; no runtime acceptance pass is claimed.

Fix-3 implementation blocker status: **none**; both required native compilation commands pass. Runtime validation remains blocked locally by unavailable Docker and pending Controller execution on Windows/PostgreSQL16. Stop after this fix, without staging, commit, push, PR, merge, or review entry.

## 5. Controller Continuation — Second Regression-IT Selector

Controller reports that the second regression-IT selector ran 338 tests: 334 passed and four failed. This is Controller-provided evidence, not a local WSL runtime rerun. Three failures are confirmed host/container clock-skew fixture issues: the Docker Linux engine is approximately 6122 ms ahead of the Windows host/JVM. Database-default service activation `created_at` exceeds JVM `updated_at`; database-default risk `observed_at` exceeds JVM `cleared_at`; SQL claim `risk_checked_at` exceeds JVM `verified_at`. The fourth failure is the obsolete exact V019 count expectation of zero in `OperationsCatalogDraftRoleLeastPrivilegeIT`; current history legitimately contains one V019 row.

Fix 4 confines remediation to the three authorized test files and implementation evidence: backdate the preexisting INACTIVE activation fixture using PostgreSQL time, use PostgreSQL time for related risk and claim-history timestamps, and expect exactly one V019 row. Database constraints, production code, migrations, PA25, Control, plan/review, OpenAPI, POM/CI, and other tests are unchanged by fix 4. Controller will rerun the three previously failing tests on Windows/PostgreSQL16; no runtime acceptance pass is claimed here. See `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-4.md` for changes, static timestamp reasoning, native validation, and handoff boundaries.
