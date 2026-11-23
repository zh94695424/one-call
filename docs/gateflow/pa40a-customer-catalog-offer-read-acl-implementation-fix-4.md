# PA40A S1 — Implementation Fix 4

- Task label: `pa40a-customer-catalog-offer-read-acl-s1-fix4-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Scope: three authorized test files, an append-only Controller continuation in the fix-3 report, and this report.

## 1. Controller Evidence

The second Controller regression-IT selector ran 338 tests: 334 passed and four failed. Controller confirmed that the Docker Linux engine clock is approximately 6122 ms ahead of the Windows host/JVM clock. Three failures therefore arise from mixing PostgreSQL timestamps with JVM timestamps, not PA40A ACL behavior: activation `created_at` exceeds `updated_at`, risk `observed_at` exceeds `cleared_at`, and claim `risk_checked_at` exceeds `verified_at`. The fourth failure is a stale exact V019 count expectation; current history legitimately contains V019. These results and clock measurements are Controller-provided evidence, not local runtime observations.

## 2. Scoped Changes And Static Timestamp Reasoning

- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java:32`: the direct preexisting head INSERT explicitly sets both `created_at` and `updated_at` to `clock_timestamp() - interval '1 minute'`. Both fixture timestamps use PostgreSQL time. With the confirmed database lead of approximately 6.122 seconds, the fixture is approximately 53.878 seconds earlier than the JVM activation time, allowing the unchanged production update to satisfy timestamp ordering. The fixture remains INACTIVE at revision 7 and still asserts the transition to revision 8 with unchanged fact counts. This bounded fixture backdating handles the confirmed skew without changing the production clock or constraints.
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java:151`: risk INSERT explicitly uses PostgreSQL `clock_timestamp()` for `observed_at`. A boolean SQL CASE produces `clock_timestamp() + interval '1 second'` for CLEARED and NULL for OPEN. Thus a new CLEARED row has a strictly later clearing timestamp using the same database clock, while OPEN retains NULL. The existing conflict target and update assignments remain unchanged: state and cleared timestamp come from `excluded`, revision increments by one, and the original observation time is retained on conflict. For an existing OPEN row, the later database clearing time also follows its earlier database observation. Risk type RECYCLED, source CLAIM_CHECK, transaction, and advisory lock are preserved; there is no JVM timestamp parameter in this method.
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java:164`: `history` delegates to `historyEnvelope`, whose INSERT now uses PostgreSQL time for both `risk_checked_at` and ACCEPTED `verified_at`. The boolean SQL CASE adds one second for ACCEPTED, giving strictly later verification, and returns NULL otherwise. Payload sealing, status, identity, request key, and other inserted fields are unchanged. No JVM timestamp parameter remains in this INSERT. Unrelated uses of `OffsetDateTime` elsewhere in the fixture are intentionally untouched.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftRoleLeastPrivilegeIT.java:36`: only the exact V019 count expectation changes from `"0"` to `"1"`. Total successful version count remains exactly 19 and `fixture.provisioningOracle()` remains intact.

Static inspection against the unchanged migration constraints confirms that the modified fixture pairs use PostgreSQL time consistently. The one-second offsets provide strict ordering under the confirmed stable clock offset; no host timestamp participates in the risk or history pairs. No constraint is removed or relaxed. Comparisons with pre-edit snapshots confirm only the intended Java hunks; the original fix-3 report remains an exact byte prefix of the appended document. Existing unrelated worktree changes are preserved.

## 3. WSL-Native Validation

Run from `backend` with `JAVA_HOME=/opt/jdk21` and `PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH`:

```bash
mvn -o -B -ntp -Pcontainer-smoke -DskipTests test-compile
```

- Native Linux Maven 3.9.9 and Temurin OpenJDK 21.0.13; no Windows executable used.
- Exit status **0**, **BUILD SUCCESS**, 36.186 seconds; completed at 2026-11-23T14:45:08+08:00.
- Production classes were up to date; 192 test source files recompiled successfully.
- Compiler notes concern annotation processing, deprecated APIs, and unchecked operations; none prevents compilation.
- This is test compilation only. No runtime IT pass is claimed.
- Log: `/tmp/pa40a-fix4-container-smoke-test-compile.log`; logs and normal ignored `backend/target` outputs are not added to the patch.

Scoped whitespace validation uses the inherited worktree metadata read-only because `.git` contains a Windows drive path:

```bash
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call10 GIT_WORK_TREE="$PWD" git diff --check -- backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftRoleLeastPrivilegeIT.java docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-3.md docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-4.md
```

Scoped `git diff --check` exits **0**, without whitespace-error diagnostics. Both untracked implementation reports are additionally covered by `git diff --no-index --check -- /dev/null <report-path>` using the same Git environment; each exits **1** for new-file differences with no whitespace-error diagnostics. LF-to-CRLF advisory warnings do not indicate whitespace errors or rewrite files. No staging is needed for these checks.

## 4. Handoff Boundary

Controller will rerun the three previously failing tests on Windows/PostgreSQL16. No local Docker/PostgreSQL runtime IT, Failsafe, full verify, or review gate is launched. Local required compilation succeeds; runtime acceptance remains pending Controller execution.

No production code, migration, PA25, Control, plan/review, OpenAPI, POM/CI, or other test is changed by this fix. No staging, commit, push, PR, merge, or review entry is performed.
