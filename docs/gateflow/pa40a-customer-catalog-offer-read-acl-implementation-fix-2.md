# PA40A S1 — Implementation Fix 2

- Task label: `pa40a-customer-catalog-offer-read-acl-s1-fix2-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Workdir: `/mnt/c/Users/Administrator/.codex/worktrees/pa40a-customer-catalog-offer-read-acl/codex-call`.
- Scope: fix the two pre-existing Windows migration-path comparisons only; stop after the fix without staging, commit, push, PR, merge, or review entry.

## 1. Controller Continuation And Root Cause

Controller reports that the corrected PostgreSQL16 `CustomerCatalogOfferReadPrivilegesIT` passes 16/16. Its rerun of the plan's 80-test focused selector exposes Windows-only failures in `CustomerSubjectActivationBoundaryTests.openApiDataDocsAndAllSeventeenMigrationsRemainFrozen` (line 56) and `CustomerSubjectClaimBoundaryTests.openApiMigrationsAndProductionProviderAbsenceRemainFrozen` (line 73). Windows `Path.toString()` emits backslashes, whereas the frozen expected paths use forward slashes. The integration result is Controller-reported, not rerun by this author.

## 2. Exact Changed Files And Preserved Guards

- `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectActivationBoundaryTests.java`: normalize only the actual migration-path mapping to `"backend/" + path.toString().replace('\\','/')`.
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectClaimBoundaryTests.java`: apply the same actual-path normalization.
- `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-1.md`: append the Controller runtime continuation without rewriting prior evidence.
- `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-2.md`: this bounded fix report.

The normalization follows `CustomerCatalogOfferReadAclBoundaryTests`. Each Java file is checked byte-for-byte against a pre-edit snapshot with exactly one expression replacement; all other bytes, including line endings, remain unchanged. Both exact order-independent migration comparisons still require the same 18 V001–V018 basenames plus authorized V019 (19 total), with no extra or missing entry accepted. All frozen literals, raw/LF SHA-256 checks, historical counts and other assertions are unchanged. No guard is removed, weakened, or replaced by a basename-only comparison.

No V019, PA25, application source, other test, Control, plan/review, POM/CI, OpenAPI, private helper, or generated artifact is authored or patched. Existing unrelated worktree changes are left intact. Maven validation writes its normal ignored `backend/target` build outputs only.

## 3. Required Native Validation

Run from `backend` with `JAVA_HOME=/opt/jdk21` and `PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH`: WSL-native Temurin OpenJDK 21.0.13 and Maven 3.9.9, offline cached dependencies. No Windows Java/Maven executable is used for validation.

```bash
mvn -o -B -ntp -Dtest=CustomerSubjectActivationBoundaryTests,CustomerSubjectClaimBoundaryTests test
mvn -o -B -ntp -Dtest=CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectClaimBoundaryTests,CustomerSubjectActivationBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsActivationReadBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogDraftBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test
```

| Validation | Exit status | Tests | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- | --- |
| Two-class selector | 0 | 12 | 0 | 0 | 0 |
| Twelve-class focused selector | 0 | 80 | 0 | 0 | 0 |

Both commands report `BUILD SUCCESS`. The first selector runs 6 activation and 6 claim tests. The focused selector runs business-number 4, subject-detail 4, evidence 4, claim 6, activation 6, operations-lifecycle 8, operations-subject-read 8, operations-IVR-session-read 8, operations-activation-read 8, operations-catalog-version 8, operations-catalog-draft 8, and evidence-role-IVR-association ACL 8: exactly 80 tests. The two selectors overlap; they are not 92 distinct tests.

Scoped whitespace validation is run from the worktree root, using the existing worktree metadata read-only because the inherited `.git` pointer uses a Windows drive path:

```bash
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call10 GIT_WORK_TREE="$PWD" git diff --check -- backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectActivationBoundaryTests.java backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectClaimBoundaryTests.java
```

Exit status: **0**, no whitespace errors. Git emits existing LF-to-CRLF advisory warnings but does not rewrite the files. The independent byte comparison confirms exactly the intended expression replacement in each Java file; a separate migration-set check confirms exactly the unchanged 19 expected basenames. The fix-1 report's pre-edit bytes remain an exact prefix of the appended report.

Native logs: `/tmp/pa40a-fix2-boundary.log` and `/tmp/pa40a-fix2-focused.log`. No test log or generated build artifact is added to the patch.

## 4. Handoff Boundary

Controller will rerun on Windows. Native success does not independently prove Windows execution or completion of any full acceptance gate. No PostgreSQL, Docker, Failsafe, full verify, or review gate is launched by this fix author.

Fix-2 blocker status: **none**. Both required native selectors and the scoped diff check are green. Controller Windows rerun remains pending, not claimed as completed. Stop after this fix.

## 5. Controller Continuation — Fix 3

Controller reports that the first regression-IT selector ran 140 tests: 138 passed, one failed, and one errored. This runtime evidence is Controller-reported, not independently rerun in WSL. The two remaining defects are historical PA25 test assumptions exposed by current V019 history: the business-number widened ACL count still expects 291 instead of current clean 312 plus one intentional grant (313), and the subject-detail hostile V016 history mutation uses installed rank 19, now occupied by legitimate V019, causing a duplicate-key SQL error before PA25 evaluates history. Fix 3 changes only those two literals to 313 and 20 respectively, preserving the surrounding guards and restoration. WSL cannot execute Docker here; Controller will rerun both Failsafe IT classes on Windows/PostgreSQL16. See `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-3.md` for the bounded change and native validation evidence.
