# PA40A S1 — Implementation Fix 5

- Task label: `pa40a-customer-catalog-offer-read-acl-s1-fix5-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Work unit: `PA40A-customer-catalog-offer-read-acl`; implementation validation, bounded fix 5.
- Exact accepted baseline and verified HEAD: `0064c86d3df704aeb02b9084143049de089606a6`.
- Design source: `docs/12-technology-stack-and-system-architecture.md`.
- Accepted plan: `docs/gateflow/pa40a-customer-catalog-offer-read-acl-plan.md`.

## Scope And Restoration

Only the following two existing files were edited, using `apply_patch`, plus this separately authorized new evidence report:

1. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java`: restored exactly to HEAD. The existing INACTIVE fixture INSERT again specifies only id, subject_id, status and revision; explicit created_at/updated_at backdating was removed.
2. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java`: restored exactly to HEAD. The risk fixture again supplies JVM `OffsetDateTime.now(ZoneOffset.UTC)` for CLEARED and null otherwise; explicit observed_at and SQL CASE timestamp substitutions were removed. `historyEnvelope` again supplies JVM `OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1)` for ACCEPTED and null otherwise. Its baseline `risk_checked_at=clock_timestamp()` expression is intentionally retained because it belongs to the accepted source, not the fix-4 workaround.

Exact baseline content was inspected only with:

```bash
git show HEAD:backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java
git show HEAD:backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java
```

Neither restored path belongs to Appendix C's exhaustive 44-path existing-test write allowlist or section 6's eight additional support/new paths. The plan explicitly identifies the activation IT as read-only and the claim fixture as a read-only startup participant. Fix 4's environment-clock workarounds therefore expanded the accepted 52 implementation paths to 54 and must not remain. The OperationsCatalogDraftRoleLeastPrivilegeIT correction and every other implementation change were preserved.

## Controller Environment Evidence

The fix-4 report records Controller-provided measurements that the Docker Linux engine was approximately 6122 ms ahead of the Windows host/JVM. Its reported regression selector ran 338 tests, with 334 passing and four failures: three timestamp-ordering failures involving activation created_at/updated_at, risk observed_at/cleared_at and claim risk_checked_at/verified_at, plus a stale V019 count expectation. Those measurements and runtime results were not observed locally in this fix.

The current Controller dispatch states that Windows host/Docker clock ordering has now been corrected and directs removal of the source workarounds. This fix relies on that Controller evidence; it does not independently measure clocks or assert runtime recovery.

## Validation Commands And Results

The checkout's `.git` pointer contains a Windows drive path that native Linux Git cannot resolve directly. Commands used these process-local environment overrides, without changing Git configuration or metadata:

```bash
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call10 GIT_WORK_TREE="$PWD"
git rev-parse HEAD
```

HEAD returned the exact accepted commit above. Both mandatory per-file checks were run after restoration and again after compilation:

```bash
git diff --quiet HEAD -- backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java
git diff --quiet HEAD -- backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java
git diff --check -- backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java
```

Every check exited **0**. Git emitted existing LF-to-CRLF conversion notices, not diff or whitespace failures. Both restored files have no implementation diff against HEAD.

Before editing, modified/untracked file bytes and path inventories were captured outside the repository:

```bash
git ls-files --modified --others --exclude-standard -z | sort -z | while IFS= read -r -d '' path; do case "$path" in backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java|backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java|docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-5.md) continue;; esac; if [ -f "$path" ]; then sha256sum -- "$path"; fi; done > /tmp/pa40a-fix5-preserved-before.sha256
git diff HEAD --name-only > /tmp/pa40a-fix5-tracked-before.paths
git ls-files --others --exclude-standard > /tmp/pa40a-fix5-untracked-before.paths
```

A read-only Python assertion check parsed the exact 44 Appendix C paths and eight section 6 paths from the accepted plan and compared their set against the current tracked implementation diff plus new backend paths. It exited **0**: **49 tracked modified + 3 new = 52 implementation paths**, exact allowlist equality, with no missing or extra implementation path. It also verified all **93** other preexisting modified/untracked files against their SHA-256 snapshot and confirmed only the two restored paths left the tracked diff.

The following independent final preservation checks ran after compilation and before this report was created:

```bash
sha256sum --quiet -c /tmp/pa40a-fix5-preserved-before.sha256
diff -u <(sed '\|^backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java$|d; \|^backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java$|d' /tmp/pa40a-fix5-tracked-before.paths) <(git diff HEAD --name-only 2>/dev/null)
diff -u /tmp/pa40a-fix5-untracked-before.paths <(git ls-files --others --exclude-standard)
test ! -e docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-5.md
```

All four exited **0**. Thus every other current intended implementation path and its bytes were preserved, along with preexisting reports/private helper bytes and the preexisting untracked path set. This new report is separately authorized evidence outside the 52-path implementation count.

### Bounded Compile

Executed from the repository root:

```bash
export JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
cd backend
set -o pipefail
mvn -B -ntp -Pcontainer-smoke -DskipTests test-compile 2>&1 | tee /tmp/pa40a-fix5-container-smoke-test-compile.log
```

- Pipeline exit **0**, **BUILD SUCCESS**; elapsed **01:04 min**; finished **2026-11-23T16:29:28+08:00**.
- Compiled 298 production source files and 192 test source files with Java release 21.
- Non-fatal diagnostics concerned annotation processing, missing Jakarta XML binding annotation enum metadata, deprecated APIs and unchecked operations.
- Only bounded compilation ran. No Docker, Testcontainers, Failsafe execution, full suite or runtime validation ran. No runtime-pass claim is made.
- Temporary logs/snapshots remain outside the patch under `/tmp`; normal ignored Maven outputs remain under `backend/target`.

## Residual Risks And Stop Boundary

Compile success does not prove PostgreSQL constraint ordering, ACL behavior, fixture runtime execution or regression success. Baseline fixture behavior again mixes JVM and database timestamps; it depends on the Controller-corrected environment clock ordering. Renewed clock skew could reproduce the original timestamp failures. Clock measurement, Docker/PostgreSQL behavior, the regression selector and all runtime validation remain Controller-owned and uncovered locally.

No other source, test, migration, PA25, README/runbook, CI, POM, OpenAPI, plan, review, control document or private helper was edited by this fix. No staging, commit, push, branch/PR creation or update, merge, approval, ready marking, reviewer request, external comment or Git configuration change occurred.

This report is written with `apply_patch` as the final tool action. No tool call or post-write validation follows it. Stop here and return control to the Controller for runtime validation.
