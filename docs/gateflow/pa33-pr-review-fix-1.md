# PA33 Draft PR #44 — bounded PR-review fix 1

## Scope and inputs

- Task: `pa33-pr44-fix1-gpt-20261114-01`.
- Bookkeeping base: `0f6b7b343dd22cf46d7003d572a6462271a3201c` (`0f6b7b34`).
- Inputs: PA33 Control tail in `project-execution-control.md` and both `pr-44-review-20261114-233718.md` / `pr-44-review-20261114-233828.md` artifacts.
- Gate: bounded test/oracle hardening for `PA33-PR-REV-01..04`, within existing fixed identities. No production behavior, product path, test class or test method is added or changed in identity.
- No production pool, scanner, URL validation, credential policy, role policy or role-handoff implementation changes. In particular, the pre-handoff Operations-role assertion remains intact as defense-in-depth.

## Accepted findings addressed

### PA33-PR-REV-01 — independent history caps

`OperationsSubjectReadsIT.detailHistoryZeroOneHundredAndOverflowAreBounded` now creates separate subjects for binding/claim counts **0/0, 1/1, 100/100, 101/1, 1/101 and 101/101**. Success cases assert both exact history sizes. Each overflow case independently asserts HTTP 503 / `SERVICE_UNAVAILABLE`, the existing exact five-property Problem envelope and privacy headers, and absence of history/projection fields, the subject identifier and sensitive/SQL canaries from the whole response. A claims-only overflow can no longer be hidden by a bindings-side failure.

### PA33-PR-REV-02 — existential risk and independent display proof

`OperationsSubjectReadsIT.allBindingRiskStatesAndAbsentActivationHaveExactProjection` now uses six distinct subjects:

1. ACTIVE binding with CLEARED risk.
2. Two DISABLED bindings: the lower-ID display binding is CLEARED and the non-display binding is OPEN.
3. RETIRED binding with CLEARED risk.
4. A binding without risk rows, projecting CLEARED.
5. ACTIVE/DISABLED/RETIRED bindings whose risks are all CLEARED.
6. An ACTIVE CLEARED display binding plus a non-display RETIRED OPEN binding, projecting OPEN.

For every subject, a real list request filtered by subject ID and a real detail request independently assert the expected `riskState` and display `maskedNumber`. Every list still asserts INACTIVE for absent activation, and every detail still asserts that `activation` is omitted. Display-only risk derivation fails the two non-display OPEN cases.

### PA33-PR-REV-03 — source confinement and stable ACL evidence

`OperationsSubjectReadBoundaryTests.ownerSqlSelectsOnlyApprovedSafeColumnsAndCounts` now extracts FROM/JOIN relation references for each adapter and requires its base-table references to use that owner's approved schema. The subject adapter's known `safe_subjects` CTE and LATERAL structure are accounted for; each adapter must still reference at least one owner-schema table. Existing safe-column/count checks remain.

The existing `OperationsSubjectReadRoleLeastPrivilegeIT.catalog()` oracle additionally hashes deterministically ordered membership tuples (`roleid`, `member`, `grantor`, `admin_option`, `inherit_option`, `set_option`) from `pg_auth_members`, and column identity/ACL tuples (`attrelid`, `attnum`, `attacl`) from `pg_attribute`. Existing role/function/table-ACL fingerprints remain.

Positive owner-role reads, real Operations-role foreign-table SQLSTATE **42501** checks and exact six non-inherited SET-membership assertions are unchanged. No assertion incorrectly denies owners tables for which the frozen migrations legitimately grant access; evidence combines adapter source confinement and unchanged ACL catalogs, not a fictional cross-schema privilege restriction.

### PA33-PR-REV-04 — real environment scanner coverage

`OperationsSubjectReadConfigurationTests.missingCredentialsAndCursorKeysFailAtAffectedRequestNotBoot` now injects enumerable `MapPropertySource` entries into the real application `ConfigurableEnvironment` and obtains properties through the existing production bean/scanner. It covers:

- `cursor.keys[1].id/material` with no index 0.
- Valid index 0 plus out-of-range `cursor.keys[3].id/material`.
- `cursor.keys[0].id` without material.
- Valid index 0 plus unexpected `cursor.keys[0].comment`.
- Valid one-entry and two-entry rings, including the second entry as active key.

All six shapes permit default/web context creation without a read pool/service. Persistence context, properties and service creation and a successful detail request are proved for every shape, using mocked JDBC/owner collaborators with the real configured transaction template and read service. Each invalid shape fails only on cursor construction or an authenticated valid list request with sanitized `SERVICE_UNAVAILABLE`; it cannot proceed to subject reads. The positive rings validate their exact entry counts, round-trip an authenticated cursor and permit list requests. Existing input-validation and disabled-employee precedence are also asserted. No scanner/binding/pool production implementation is changed.

## Validation

Native Linux tooling: Eclipse Temurin **21.0.13+9-LTS** at `/opt/jdk21`, Apache Maven **3.9.9** at `/opt/maven-3.9.9`, explicitly using `/root/.m2/repository`; Docker Engine **29.1.3**, disposable PostgreSQL 16 fixtures. Commands run from `backend/`:

```bash
JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH mvn \
  -Dmaven.repo.local=/root/.m2/repository -B -ntp \
  -Dtest=OperationsSubjectReadConfigurationTests,OperationsSubjectReadBoundaryTests,ModuleBoundaryTests,CustomerEvidenceBoundaryTests,OperationsSubjectReadServiceTests \
  test

JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH mvn \
  -Dmaven.repo.local=/root/.m2/repository -B -ntp -Pcontainer-smoke \
  -Dit.test=OperationsSubjectReadsIT,OperationsSubjectReadRoleLeastPrivilegeIT verify
```

| Validation | Result |
| --- | --- |
| Focused unit suite | **45/45**, zero failures/errors/skips, BUILD SUCCESS, **01:02**; configuration 8 + read boundary 8 + module boundary 9 + customer evidence boundary 4 + service 16 |
| Container-profile Surefire and targeted Failsafe | **644/644 unit + 36/36 targeted container**, zero failures/errors/skips, BUILD SUCCESS, **04:33**; Failsafe read 24 + least-privilege 12 |
| Exact static inventory | **70/644 Surefire + 29/476 Failsafe = 99/1120**, line-scan tokens **1121**, POM Failsafe includes **29** |
| PA33 fixed identities | All **106 = 70 unit + 36 container**, exact method names/order unchanged from `0f6b7b34` |
| Frozen boundary | **22/22 raw-byte SHA256 hashes** match implementation Appendix B; exactly **V001–V017**, no migration/OpenAPI/frozen-document diff |

The focused run completed at **2026-11-14 23:55:03 +08:00**; container verification completed after local midnight at **2026-11-15 00:00:15 +08:00**. The task/evidence label remains `20261114`; neither run is hosted CI.

Evidence root outside the repository: `/tmp/pa33-pr44-fix1-20261114/`. `focused.log` and isolated `focused-xml/` retain the successful focused run. Report directories were removed before container verification so stale XML cannot count as fresh execution. `container.log` and isolated `container-unit-xml/` / `container-xml/` retain the container-profile run. `audit.py` / `final-audit.log` execute the checked-in CI static inventory, independently compare every exact class/method set, compare PA33 identities to the bookkeeping base, and check fresh XML against exact CI identities with zero failures/errors/skips. `frozen-sha256.txt` / `frozen-check.log` retain all 22 raw-byte hash checks. `diff-check.log` records the clean tracked whitespace check; the audit also checks the new artifact's whitespace and exact seven-path scope. POM/CI and all production source remain unchanged, and staged diff is empty.

Initial focused validation reported **45 tests / 0 failures / 1 error / 0 skips**: the new mocked JDBC connection lacked the `Statement` needed by the real transaction manager's enforced READ ONLY setup. A one-method diagnostic rerun reproduced the error; an instrumented rerun reported one assertion failure locating it before authority/owner reads. Adding only that JDBC statement mock resolved the fixture setup; temporary diagnostic instrumentation is removed. These unsuccessful runs remain in `focused-initial-failure.log`, `config-diagnostic.log` and `config-diagnostic-2.log`; they are not relabeled as passes. Ordinary Mockito dynamic-agent/JVM-sharing and compiler diagnostics are not test failures.

## Changed paths and invariant boundary

Exactly seven intended repository paths relative to `0f6b7b34`:

1. `backend/src/test/java/com/onecall/archtests/OperationsSubjectReadConfigurationTests.java`
2. `backend/src/test/java/com/onecall/archtests/architecture/OperationsSubjectReadBoundaryTests.java`
3. `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadsIT.java`
4. `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java`
5. `docs/gateflow/pa33-pr-review-fix-1.md` (new gate artifact)
6. `docs/gateflow/pa33-operations-subject-read-foundation-implementation.md` (test-evidence and capacity wording only)
7. `docs/gateflow/pa33-operations-subject-read-foundation-draft-pr-readiness.md` (test-evidence and capacity wording only)

The implementation/readiness wording distinguishes the **100-batch/10,100-candidate fail-closed scan bound** from a guaranteed success target within five seconds: usable capacity is database-RTT/query/role-handoff/budget dependent. Role assertions are not optimized away. No production source, fixture, grant, plan, Control, review artifact, POM, CI, migration, OpenAPI or docs/14/15/17 is edited. Existing private `.codex` and `.pa33-plan-fix1-prompt.md` remain preserved and excluded; no staging or commit is performed. The Windows `.git` pointer is not rewritten; read-only Git audits use the existing `/mnt/d/codex-call/.git/worktrees/codex-call17` metadata with an explicit worktree and CRLF-aware settings.

## Residuals and stop boundary

- `PA33-AGG-03` remains deferred to separately authorized Contract/OpenAPI + Security/Data work. Frozen contract metadata is untouched.
- Backend/Platform/Database retain deployed RTT/query/pool/index/SLO, cooperative deadline and non-PII observability work; no admission, scanner, URL or role policy is changed.
- Identity/Platform/Security retain production employee verification, six-role login provisioning and cursor-key custody/distribution/rotation. Disposable tests are not production identity or load acceptance.
- The other **440 Failsafe identities**, hosted clean-Linux CI, packaged-JAR smokes and contract/firewall validators are not freshly rerun by this fix. No production-readiness or independent re-review acceptance claim is made.
- No push, external comment, reviewer request, approval, ready marking, merge or external issue/PR mutation is performed. Stop for fresh independent A/B PR re-review; this artifact does not self-approve the gate.
