# PA36 PR #46 bounded fix 3

## Identity and scope

- Task: `pa36-pr46-fix3-gpt61sol-20261117-01`; work unit: `PA36-evidence-role-ivr-association-acl`.
- Date: 2026-11-17, Asia/Shanghai. Gate remains **PR review -> fix**.
- Verified entry HEAD: `d5badecf74f458bd54de2987c84069d6b2a4219e`.
- This round changes only `evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn` in `backend/src/test/java/com/onecall/archtests/container/EvidenceRoleIvrAssociationAclIT.java` and adds this sole artifact. All other methods, test identities, production code, V001-V018, PA25, POM, CI, existing docs, fix-1/fix-2 artifacts and Control remain unchanged from round entry.

## Controller result and finding disposition

The controller reports the final pre-fix candidate full run on authorized Java/Docker/PostgreSQL16:

| Suite | Classes | Tests | Failures | Errors | Skips |
| --- | --- | --- | --- | --- | --- |
| Surefire | 77 | 712 | 0 | 0 | 0 |
| Failsafe | 32 | 520 | 1 | 0 | 0 |

The sole failure was `EvidenceRoleIvrAssociationAclIT.evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn:640`, at the forbidden-delegation `denied(...)` call. These are controller-provided pre-fix results, not a local run or a post-fix success claim. The actual ACL facts remained correct.

`PR46-C-01` remains an **accepted test-gap finding**: the delegation attempt must demonstrably confer no privilege. Its initial expected-42501 implementation is **rejected-with-reason**: PostgreSQL can emit a warning and grant nothing when the executing role lacks grant option, without raising a SQL exception. Requiring SQLSTATE `42501` incorrectly rejects that valid warning/no-op outcome.

The final probe creates a JDBC savepoint on the existing connection under `SET LOCAL ROLE evidence_role`, then executes the unchanged SQL:

```sql
GRANT SELECT (call_id) ON evidence.evidence_record TO customer_iam_role
```

Before any rollback, that same connection and transaction executes:

```sql
SELECT has_column_privilege('customer_iam_role', 'evidence.evidence_record', 'call_id', 'SELECT')
```

The test requires a result row and asserts `result.getObject(1, Boolean.class)` equals exactly `false`, rejecting both `true` and SQL NULL. The `finally` block rolls back to the savepoint; the existing final connection rollback and `assertThat(acls()).isEqualTo(currentOracle())` remain in place. No warning text is inspected, and this delegation probe neither requires an exception nor classifies the outcome as SQLSTATE `42501`. Other denied-operation assertions are untouched.

This targeted behavioral probe does **not** replace the independent 410-tuple ACL oracle or the existing grant-option checks.

## Validation and invariants

- `cd backend && JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests test`: exit 0, **BUILD SUCCESS; 1 class / 8 tests / 0 failures / 0 errors / 0 skips**. Test compilation includes the corrected integration test. Finished 2026-11-17 00:49:10 +08:00.
- `docker info`: exit 1, permission denied accessing `unix:///var/run/docker.sock`. The requested `cd backend && JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=EvidenceRoleIvrAssociationAclIT#evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn verify` is **NOT EXECUTED**. No socket, permission, configuration or sandbox workaround is attempted; controller owns the PostgreSQL focus and subsequent full run.
- Independent static checks preserve all ordinary `@Test` identities, the named method identity and every byte outside that method relative to round entry. The old expected-42501 delegation call is absent. No test is skipped or renamed.
- V018 repository-canonical LF SHA256 remains `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`.
- Independent PA25 array/topology parsing and the passing boundary tests confirm unchanged ACL **53/290/18/410** (customer role / seven-role union / evidence role / complete oracle), history **18**, and topology **7 logins / 9 exact-ACL-monitored roles / 11 approved edges**, including seven shared Operations edges. These are static invariants, not a local PostgreSQL execution claim.
- Scope-count discrepancy: the supplied workspace already contains **11 candidate paths** at entry: nine modified tracked files plus the fix-1 and fix-2 artifacts. Preserving that exact existing candidate and adding the mandatory fix-3 artifact necessarily yields **12 final candidate paths**, excluding the pre-existing untracked `.codex`. Claiming 11 after this addition would be incorrect; no existing path is removed or altered to force that count. Only the named test file changes this round, plus this new artifact.
- `git diff --check` and exact candidate-path checks use the explicit read-only linked-worktree Git prefix. Untracked artifacts are separately checked for UTF-8, BOM absence and trailing whitespace. The index remains unchanged. Local command logs and integrity evidence are in `/tmp/pa36-pr-fix3-evidence/`.

## Residual owners and destinations

- **Controller / authorized validation runtime:** execute the named PostgreSQL16 Failsafe focus and the final full candidate run; resolve the stated 11-versus-12 path accounting without dropping preserved artifacts. Local Docker denial is not a passing container result.
- **Controller / hosted CI:** assess exact-head hosted CI in its separately authorized workflow; no hosted success or PR action is claimed here.
- **Technical Documentation + Backend/DBA / docs14 deferred work:** retain `PA36-AGG-B-01` for a separately authorized current-heading/evidence-column update and dependent pins; no docs14 changes here.
- **Platform/Security/Operations / existing production-readiness gates:** credentials, TLS/pg_hba, rotation and old-password refusal, pool drain/stale sessions, marker custody and exclusive/cluster administration remain owner-routed, not executed here.
- **PA35 owner / application behavior gate:** application-side IVR association behavior remains outside this bounded fix; PA35 remains paused.

## Stop boundary

`author_code_complete=true`

`controller_full_run_pending_after_fix=true`

No `git config`, staging, commit, push, PR mutation, approval, mark-ready, reviewer request, external comment, merge, Control mutation, re-review or other gate is performed or entered. Stop after the single corrected test and this fix-3 artifact at **PR review -> fix**; no acceptance or production-readiness claim is made.
