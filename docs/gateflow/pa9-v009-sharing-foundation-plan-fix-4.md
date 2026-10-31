# PA9 V009 Sharing Persistence Foundation — Plan Fix 4

- Task label: `pa9-v009-planfix4-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`.
- Gate: `fix` — bounded helper-name/call-site correction only; date: 2026-10-31, Asia/Shanghai.
- status: blocked
- No plan acceptance, implementation authorization, review pass or production-readiness claim. This records an input/authority blocker for the accepted `F-PA9-RR4-01` / B round-4 finding 01, not a new finding or authorization to expand the fix.

## 1. Inputs and plan identity

Read the current plan, fix-1/fix-2/fix-3, the active checkpoint in `docs/gateflow/project-execution-control.md`, latest round-4 A `docs/reviews/plan-review-20261031-083645.md`, latest round-4 B `docs/reviews/plan-review-20261031-083810.md`, and the actual migration setup, helper definitions and inventory call sites in `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`.

| Plan identity | SHA256 |
| --- | --- |
| Pre-fix plan | `8e23c4e46e1dd9689c59a4c8c19169870844263602b4d886161ed26229aebc0c` |
| Post-task plan, restored byte-for-byte | `8e23c4e46e1dd9689c59a4c8c19169870844263602b4d886161ed26229aebc0c` |
| Actual unchanged `PostgresContainerIT.java` | `82ba4ede3184b1506e90f64c077da95a3ccaf1eee1daa0c9db3c61ada77fe8bf` |

The source hash matches both round-4 reviews' frozen source identity; this is not a different test-file revision. The plan's supplied pre-fix hash matched before editing. A draft confined to §7.2 was checked against that hash using an in-memory reconstruction and unified diff. Validation exposed the authority conflict below; the draft was withdrawn with `apply_patch` and the full original plan hash verified. No draft correction remains in the delivered plan.

## 2. Exact blocker and required authority clarification

The task and active control prohibit treating `assertRiskOpenClearReopenRevisionProtocol` as a sharing-absence site and assert that it contains no such assertion. The actual unchanged method starts at `PostgresContainerIT.java:17988`, is invoked at `:2337`, and contains this assertion at `:18125–18128`:

```java
assertThat(queryLong(connection,
        "SELECT count(*) FROM pg_namespace WHERE nspname = 'sharing'"))
        .as("no sharing schema")
        .isZero();
```

B round-4 finding 01 explicitly confirms this same source location and says the risk helper was correctly named; A's suggested correction also retains its capture. Thus the supplied exclusion conflicts with both the actual frozen source and the reviewed correction. Leaving that assertion unchanged after unconditional V009 leaves a deterministic failure in the existing V003 test path. Describing it as nonexistent would be factually false; silently changing its planned responsibility would exceed the explicit bounded authority.

The three requested V006/V007/V008 call-site corrections and a non-test staged/final split are feasible without changing test counts. The blocker is authority to account for the remaining actual post-final assertion while preserving cumulative tests, not a need for new DDL, test methods or product behavior. Stop here: the controller must reconcile the task/control exclusion with the unchanged source, either authorizing the corresponding staged capture/final-state correction or supplying a corrected frozen source/authority baseline. No broader fix is assumed or performed.

## 3. Verified helper semantics and staged/final matrix

All source anchors refer to the current unchanged `PostgresContainerIT.java`; routes below describe the requested bounded correction, not delivered implementation or an accepted plan amendment.

| Stage / current location | Verified semantics and bounded routing |
| --- | --- |
| Current migration setup `:934` onward; unrestricted V008 step `:1088–1092` | Existing stages end at 007. The requested explicit target-008 checkpoint would call both current absence helpers before unrestricted V009, capture real successful results, and preserve all earlier staged evidence. Neither staged helper should run against final V009. |
| `assertNoV008PlusObjects`, definition `:8262` | Requires exact six IVR, seven entitlement, three activation-consent and five evidence table sets; two evidence roles; only `issue_first_trial` as the entitlement SECURITY DEFINER service; exactly five global SECURITY DEFINER services; listed future-service and legacy IVR-name absence; late consent FK present. Requires sharing schema/roles absent and all three sharing table names absent in activation_consent, entitlement_metering, evidence and sharing. Preserve all these assertions at staged 008, despite the historical helper name. |
| `assertNoV009PlusObjects`, definition `:23962` | Requires sharing schema/roles absent, null regclass for the three sharing tables, and exactly five global SECURITY DEFINER services. Preserve at staged 008 only. |
| Final `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact`, definition `:2453`, call `:2459` | Currently calls `assertNoV008PlusObjects`. The requested correction replaces that call with an exact live V009/no-V010 fence while preserving all other V006 assertions and captured staged evidence. |
| Final `v007ActivationConsentInventoryAndGuardShapesAreExact`, definition `:2506`, call `:2512` | Currently calls `assertNoV008PlusObjects`. The requested correction replaces that call with the same live fence while preserving all other V007 assertions and captured staged evidence. |
| Final `v008EvidenceInventoryAndGuardShapesAreExact`, definition `:2589`, call `:2595` | Currently calls `assertNoV009PlusObjects`. The requested correction replaces that call with the same live fence while preserving evidence catalog/ACL/FK/control, staged-prefix and digest evidence. |
| `assertV006PresenceAndLateFk`, definition `:8343`, V005 inventory call `:2399` | No sharing-absence assertion; preserves entitlement/activation presence and late-FK evidence. No sharing-absence correction belongs here. |
| Risk helper, definition `:17988`, call `:2337` | Source/authority conflict in §2 prevents a complete cumulative staged/final routing contract under the supplied exclusions. No edit authorized or performed. |

The requested live fence must retain all prior non-sharing-absence checks, cross-schema misplaced-name exclusions and complete global inventories. It must assert exact sets of the three sharing tables, two roles, three owner-local functions and four qualified user triggers, nine migration resources/history rows and no V010 expansion, not just counts. The five SECURITY DEFINER services remain `audit.append_fact`, `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append`, `entitlement_metering.issue_first_trial`; no sharing service expansion is permitted.

The existing §7.2 requirements for `assertSchemasAndRoles` (`:6085`) and `assertFunctionsAndTrigger` (`:6854`) are otherwise retained: add `sharing=sharing_owner`, `sharing_owner`, `sharing_role` to exact global sets, remove post-final zero-sharing checks, and append exactly the three sharing functions and four qualified sharing user triggers to complete prior lists. Their absence evidence belongs at the genuine staged-008 checkpoint, not in constants. No additional helper name is adopted in the delivered plan while blocked.

## 4. Unchanged scope and exact delivered changes

- No delivered plan section or call-site change. The §7.2 draft was fully restored; §7.1, lifecycle witnesses, prior fixes and every other plan section remain byte-identical to intake.
- Frozen DDL/ACL/inventory unchanged: three tables, 38 columns, 31 checks, six FKs, nine defaults, five secondary and 13 physical indexes, three sharing functions and four sharing triggers; no V010, schema, service or product expansion.
- Test contract unchanged: three slices; exactly seven planned additions plus `v001ThroughV008MigrateCleanPostgres16WithoutSkipping` → `v001ThroughV009MigrateCleanPostgres16WithoutSkipping`; Failsafe 57→60→62→64 and ten Surefire tests. Static source check found 57 actual `@Test` annotations. No test method was added, renamed, removed or executed.
- Actual delivered changed-path list: created `docs/gateflow/pa9-v009-sharing-foundation-plan-fix-4.md` only. The plan was temporarily edited within its permitted path and restored exactly. Source, migrations, CI, control, prior artifacts and all other paths are unchanged.

## 5. Diff, whitespace and boundary validation

- Full pre/post plan SHA256 equality proves zero delivered plan diff. The withdrawn draft's unified diff was restricted to §7.2; no call site was actually edited.
- A sorted path/content SHA256 manifest excluding the two permitted task paths covers 1,025 files. Before and after digest: `f3d3e299be65fe7b4441d1f009f3107d2ce0f310a329c54d6733a333cdcfb9b7`. Existing controller dirt and untracked artifacts are preserved, not claimed as task writes.
- Read-only `git diff --check` returned 0. Explicit `git -c core.autocrlf=false diff --no-index --check /dev/null <path>` for each task document emitted no whitespace diagnostics and returned 1 for new-file differences; an independent trailing-whitespace/final-newline check passed. Use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0` for all Git checks; the automatic Windows pointer is broken and was not repaired. An LF→CRLF checkout warning is informational, not a whitespace error.
- An exploratory static assertion that both excluded helpers contained no `sharing` reference failed on the risk helper; direct source inspection and the frozen source hash established the blocker. This was not a runtime test or a claimed pass. The unqualified no-index Git check also hit the pre-existing broken pointer and was rerun successfully with explicit metadata.
- No implementation, Maven, Docker, PostgreSQL, network, mutating Git, stage, commit, push, PR, approval or external comment occurred. No other file was written, including no temporary baseline file.

Final state: `status: blocked`. Resume only after explicit reconciliation of the source/authority conflict in §2; the accepted finding remains unresolved by this task.
