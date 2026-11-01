# PA9 V009 Sharing Persistence Foundation — Plan Fix 3

- Task label: `pa9-v009-planfix3-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`.
- Gate: `fix` — bounded final plan-completeness fix only; author date: 2026-10-31, America/Los_Angeles.
- status: plan-fix-complete
- No implementation, acceptance, approval, production-readiness or later-gate claim is made.

## 1. Inputs and hashes

Read the current plan, fix-1, fix-2, controller adjudication in `docs/gateflow/project-execution-control.md`, round-3 A/B artifacts `docs/reviews/plan-review-20261031-081856.md` and `docs/reviews/plan-review-20261031-081706.md`, frozen design doc-14, V008 migration, cited `PostgresContainerIT.java`, CI, POM and README facts. The supplied pre-fix plan hash matched before editing.

| Artifact | SHA256 |
| --- | --- |
| Pre-fix `docs/gateflow/pa9-v009-sharing-foundation-plan.md` | `951a27df6db1d36a6acb44f08e8d0dead114584753fc8ad1aba89c6c8844ae5a` |
| Post-fix `docs/gateflow/pa9-v009-sharing-foundation-plan.md` | `8e23c4e46e1dd9689c59a4c8c19169870844263602b4d886161ed26229aebc0c` |
| Fix-3 artifact | computed after final write |

The read-only explicit Git metadata resolves HEAD to `835f2ecece1ac8b5c79ab794427424ea380ac052`; the Windows worktree pointer was not repaired. Baseline bytes and a complete file manifest were captured outside the repository before editing.

## 2. Exact sections and accepted-item mapping

| Accepted item | Plan section changed | Bounded resolution |
| --- | --- | --- |
| A `PA9-RR3-01` early-expiry refusal | §7.1 S2 method row; §7.4 lifecycle witnesses | Add separate rollback-isolated `ACTIVE → EXPIRED` and `LOCKED → EXPIRED` attempts with future OLD expiry, `revocation_generation + 1`, `revision + 1`, exact `P0001/SHARE_GRANT_EXPIRY_INVALID`, zero mutation, guard enabled, transaction/fresh-connection restoration and no admission/event residue. |
| B round-3 finding 01 same-state non-terminal generation increase | §7.1 S2 method row; §7.4 lifecycle witnesses | Add `LOCKED → LOCKED` with only generation and revision incremented; expect one-row structural success and no SQLSTATE; classify service-forbidden, retain terminal successes and generation-decrease negative. |
| A `PA9-RR3-02` cumulative inventory/absence bookkeeping | §7.2 cumulative migration chain and staged/live fence | Name exact current helpers: `assertSchemasAndRoles`, `assertFunctionsAndTrigger`, `assertNoV009PlusObjects`, `assertV006PresenceAndLateFk`, `assertRiskOpenClearReopenRevisionProtocol`, and `v001ThroughV008MigrateCleanPostgres16WithoutSkipping`; enumerate their staged/live responsibilities and source=CI=XML exact-set update. |
| Fixture clarification | §7.4 lifecycle witnesses | Permit only a method-local direct INSERT for valid synthetic future-expiry `EXPIRED`-target fixtures, explicitly not a production creation path, with cleanup and no-residue assertions. |

No DDL, ACL, table, column, migration, service, slice or test-count expansion was introduced.

## 3. Exact witness contract

The new S2 witnesses remain inside `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops`; no new test method or slice is added. Both early-expiry cases use otherwise valid fields, a future `OLD.expires_at`, status `EXPIRED`, `revocation_generation = OLD + 1`, `revision = OLD + 1`, and unchanged other fields. With the frozen guard enabled, each must raise exactly `P0001/SHARE_GRANT_EXPIRY_INVALID`, mutate zero rows, then pass `ROLLBACK TO SAVEPOINT`, transaction `ROLLBACK`, and fresh-connection exact baseline row/history plus unchanged admission/event counts. The direct INSERT fixture is cleaned in method-local cleanup and is never described as a production path.

The non-terminal witness updates a valid `LOCKED` fixture with only `revocation_generation = OLD + 1` and `revision = OLD + 1`. It expects exactly one row, no SQLSTATE, unchanged status and all other columns, then the same savepoint, transaction and fresh-connection rollback checks. It is service-forbidden, not an owner action or exactly-once proof. The controller-deferred non-LOCK mixed-action probes are not added; the matrix remains explicitly non-exhaustive and that residual is owned by `Sharing/Backend` for excluded service action discipline.

## 4. Cumulative bookkeeping contract

The S1 checklist now requires:

- `assertSchemasAndRoles` exact schema/role sets to include `sharing=sharing_owner`, `sharing_owner` and `sharing_role`, with no false post-V009 zero-sharing assertions.
- `assertFunctionsAndTrigger` to retain prior complete global inventories and add exactly three sharing functions (`sharing.guard_share_grant`, `sharing.guard_share_admission`, `sharing.reject_row_mutation`) and four sharing user triggers (`trg_share_grant_guard`, `trg_share_admission_guard`, `trg_share_admission_immutable`, `trg_share_access_event_immutable`) by qualified table/name, never count-only.
- `assertNoV009PlusObjects` to remain only at the staged target-008 checkpoint; the post-V009 live fence to assert three sharing tables, two roles, three functions, four user triggers, no V010 and global five SECURITY DEFINER services.
- `assertV006PresenceAndLateFk` and `assertRiskOpenClearReopenRevisionProtocol` sharing-absence checks to be captured before target 009, not asserted after V009.
- `v001ThroughV008MigrateCleanPostgres16WithoutSkipping` to be renamed for the staged target-008→V009 chain, with the source `@Test` set, CI Python literal set and Failsafe XML exact set kept equal at 64; intermediate checkpoints remain 60 and 62. This is test/CI bookkeeping only.

## 5. Unchanged boundaries and validation

- Frozen DDL/ACL/inventory remains unchanged: three tables, 38 columns, 31 checks, six FKs, nine defaults, five secondary and 13 physical indexes, three sharing functions and four sharing triggers; no V010.
- Slice and test-count contract remains three slices, seven added methods plus one rename, Failsafe 57→60→62→64, and exactly 10 Surefire tests.
- V001–V008 bytes, V008 migration, design, CI/POM/README source inputs and all prior plan/fix/review artifacts are unchanged.
- No Maven, Docker, PostgreSQL, network or mutating Git command was run; no stage, commit, push, PR, issue, approval, comment or implementation occurred.

## 6. Changed paths and checks

Task-owned changed paths are exactly:

1. Modified `docs/gateflow/pa9-v009-sharing-foundation-plan.md`.
2. Created `docs/gateflow/pa9-v009-sharing-foundation-plan-fix-3.md`.

`diff -u` against the captured pre-fix plan shows only the listed §7.1, §7.2 and §7.4 additions/clarification. Read-only `git diff --check` cannot use the broken automatic Windows `.git` pointer; explicit `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0` metadata is used without mutation. The final validation runs explicit no-index whitespace checks for both task files, a complete before/after file manifest, and recomputes the two task-file hashes. No unrelated path is claimed as task-owned; pre-existing controller dirt and review artifacts are preserved.

Deferred item: B round-3 finding 02, non-LOCK mixed-action probes, remains a classified residual with owner/destination `Sharing/Backend` → excluded service action discipline. The current matrix remains non-exhaustive. Final state: `status: plan-fix-complete`.
