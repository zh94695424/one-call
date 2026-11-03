# PA13 sharing-role least-privilege plan fix 3

- Task label: `pa13-sharing-role-least-privilege-plan-fix3-gpt-20261103-203539`
- Work unit: `PA13-sharing-role-least-privilege`
- Gate: `fix`, entry `plan-fix-3`
- Status: `plan-fix-3-complete`
- Date: 2026-11-03

## Binding goal

V001–V009 remain byte-identical; the sole migration is forward-only `V010__sharing_role_least_privilege.sql`. `sharing_role` is the exact read-only creator-scoped `/api/v1/customer/shares` principal. `sharing_command_role` retains the frozen future command/admission/lifecycle semantics and is not production-wired. No source/API/OpenAPI behavior change, view, SECURITY DEFINER, command implementation, or later-gate action is authorized.

## Finding disposition

- **A F-A-01 / B2-08 closed:** exact post-V010 three-role tuple/attribute fence in `assertV009LiveFence`; `sharing_command_role` added to `assertNoV009PlusObjects` and `stagedV007SharingRolesAbsent` absence fences, preserving staged counts and all 64 method names.
- **B2-01 closed:** exact future-write helper set is `v009Fixture`, `v009S2Defaults`, `v009S2Action`, both `v009S2Mutation` overloads, `v009S2FrozenColumns`, and `v009S3Configure`, plus future-write callers among the exact six named V009 methods in the plan. `v009S2KeysAndNulls` and `v009S2Shape` remain `sharing_owner` for DDL/trigger-disable/owner-only checks. Creator-query checks stay on `sharing_role`; SQL/assertion meanings and method counts remain unchanged.
- **B2-02 closed:** `assertV009AclCatalog` now prescribes every catalog/effective-privilege delta, including role tuples/membership, closed-world guard, namespace/table/column/procedure ACLs and effective sets, all privilege families, `aclexplode(coalesce(acl, acldefault(...)))`, exact query matrix, preserved command matrices, removal of stale query rows, and independent exact new-IT proof.
- **A F-A-02 / B2-03 closed:** documentation delta is the union of all README, docs/14, and CI sites enumerated in the plan. README line 40's cumulative post-V010 inventory is 67 Surefire/13 classes, 100 Failsafe/5 classes, 167 combined. PA9/PA12 counts are historical; current totals preserve 64 unchanged PostgresContainerIT methods and 67 Surefire.
- **B2-04 closed:** precise existing fixture/method/line placements retain read-only `25006`, add writable query-role revision-update denial `42501` through existing rollback/savepoint idioms, preserve CustomerMe's independent authorized `25006`, and add no test method.
- **B2-05 closed:** exact new IT path/FQCN, container/admin/test-login harness, independent byte/LF/SHA helpers, nine canonical-LF legacy digest literals, and exactly eight plain method names are pinned.
- **B2-06 closed:** “unwired” is scoped to production beans, Spring profiles, endpoints and logins; synthetic test-only logins may be members. docs/14 §2.12 must state all five command-role attributes and deliberate `NOINHERIT` deviation.
- **A F-A-03 / B2-07 closed:** fix-2 line 39 now distinguishes the review snapshot/archive CRLF bytes from the LF authoring worktree and correctly labels raw-CRLF and canonical-LF hashes.

## Preserved implementation constraints

The plan retains the exact query and command grants, eight UPDATE columns, all 34 cross-module SELECT columns, exact function signatures, owner/grantor sequence, explicit table and column revokes, no CASCADE, PUBLIC zero, and no unrelated privileges. V010 remains one transaction and the only new migration. `migrationsExecuted` changes 1→2, Flyway history is 10, exactly 64 existing PostgresContainerIT methods remain, exactly eight new IT methods are added, and inventories are 100 Failsafe, 67 Surefire, 167 combined. No role re-creation or stale cluster role can pass the new pre/post migration fences silently.

## Validation and scope

- Cross-checked the active resume checkpoint and re-review-2 adjudication in `docs/gateflow/project-execution-control.md`, both accepted reviews `docs/reviews/plan-review-20261103-202025.md` and `docs/reviews/plan-review-20261103-195101.md`, current plan/fix-2, and the directly relevant frozen source, tests, migrations, README, docs/14, POM, and CI inventory sites.
- Verified the six V009 methods, helper names/roles, read-only witnesses and SQLSTATE placements against source. Verified all eight new IT method identifiers and the exact accepted documentation-site union are present in the plan.
- Docker/Maven execution is not required in this plan gate and was not run. Runtime migration/catalog validation remains an implementation-time requirement; do not represent it as passed here.
- Exactly the three authorized files were written: the plan, fix-2 solely for hash-label correction, and this fix-3 artifact. No source, migration, test, POM, CI, README, docs/14, control, review, Git metadata, private helper, or external state was changed. No staging, commit, push, PR/review action, implementation, or re-review dispatch occurred.
- Residual risk: PostgreSQL catalog behavior, grantor identity under supported production migration credentials, Docker-backed tests, and complete final implementation inventory still require runtime validation after this plan gate.

## Final artifact digests

Record raw-CRLF (exact bytes) and canonical-LF (CRLF normalized to LF) SHA-256 for each final artifact in the author completion report after all writes. The authoring worktree may be LF while a review/archive snapshot materializes CRLF; never label one representation as the other.

`status: plan-fix-3-complete`
