# PA9 V009 Sharing Persistence Foundation — Plan Fix 5

- Task label: `pa9-v009-planfix5-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`.
- Gate: `fix` — bounded helper correction; date: 2026-10-31.
- status: plan-fix-complete
- This corrects only F-PA9-RR4-01 / B round-4 finding 01 in the plan. It is not plan acceptance, implementation authorization, a review pass, or production-readiness evidence.

## 1. Inputs and identity

Read the current plan, fix-1 through fix-4 (including fix-4's blocked record), `docs/gateflow/project-execution-control.md`, A and B round-4 reviews (`docs/reviews/plan-review-20261031-083645.md` and `docs/reviews/plan-review-20261031-083810.md`), and the actual `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` source. The task's controller-resolution instruction removes fix-4's former blocker and explicitly authorizes the corrected risk-helper accounting in the bounded plan contract.

| Identity | SHA256 |
| --- | --- |
| Pre-fix current plan | `8e23c4e46e1dd9689c59a4c8c19169870844263602b4d886161ed26229aebc0c` |
| Post-fix current plan | `392937a4fbf7d31c7d27aa17d5468c1d07334d58b04f8e996f15ae0422860530` |
| Unchanged `PostgresContainerIT.java` | `82ba4ede3184b1506e90f64c077da95a3ccaf1eee1daa0c9db3c61ada77fe8bf` |

The supplied pre-fix plan hash matched before editing. The source hash matches the round-4 frozen source identity.

## 2. Corrected plan contract

Only §7.2 was corrected. The plan now:

- Removes the false claim that `assertV006PresenceAndLateFk` holds a sharing-absence assertion; it explicitly remains unchanged for sharing concerns.
- Identifies the real `assertNoV008PlusObjects` absence checks and its V006/V007 final-database call sites, and also identifies the V008 `assertNoV009PlusObjects` call.
- Captures the genuine sharing-absence assertions from both absence helpers at the staged target-008 checkpoint, preserving their remaining exact checks and earlier staged evidence; neither staged helper/call runs against final V009.
- Routes all three final inventory call sites (`:2459`, `:2512`, `:2595`) to the same exact live V009/no-V010 fence, retaining exact sharing schema/roles/tables/functions/user-trigger sets, five SECURITY DEFINER services, nine migration rows/resources, and no V010 expansion.
- Acknowledges the real direct sharing-schema assertion in `assertRiskOpenClearReopenRevisionProtocol` at `:18125`: capture its absence at staged target 008, then remove or replace only that direct assertion so the risk test remains green after V009, preserving every non-sharing risk-transition assertion and behavior.
- Retains exact global schema/role and function/trigger inventory requirements, and retains the full source=CI=XML 57→60→62→64 Failsafe progression (one rename, seven additions) and ten Surefire tests.

No remaining source contradiction was found against the corrected contract. Relevant code anchors: `assertNoV008PlusObjects` definition `:8262`, `assertNoV009PlusObjects` definition `:23962`, `assertRiskOpenClearReopenRevisionProtocol` definition `:17988` and call `:2337`, direct risk assertion `:18125`, global schema/role inventory `:6085`, and function/trigger inventory `:6854`.

## 3. Staged/final helper matrix

| Helper / current use | Staged target 008 | Final V009 |
| --- | --- | --- |
| `assertNoV008PlusObjects` (`:8262`; calls `:2459`, `:2512`) | Capture its real sharing schema/roles/tables absence while V008 is present and V009 absent; preserve exact prior object sets, evidence V008 presence, and other evidence/function-wave checks. | Do not run the staged helper/calls. Replace both retained V006/V007 calls with the exact live V009/no-V010 fence. |
| `assertNoV009PlusObjects` (`:23962`; call `:2595`) | Capture its real sharing schema/roles/tables absence and preserve its five-service check. | Do not run the staged helper/call. Replace the retained V008 call with the same exact live V009/no-V010 fence. |
| `assertRiskOpenClearReopenRevisionProtocol` (`:17988`; call `:2337`; direct absence at `:18125`) | Capture the direct sharing-schema absence at the staged target-008 checkpoint. | Remove or replace only the direct sharing-absence assertion; retain all non-sharing risk-transition assertions and behavior. |
| `assertV006PresenceAndLateFk` (`:8343`; call `:2399`) | No sharing-specific change. | No sharing-specific change; it contains no sharing-absence assertion. |
| `assertSchemasAndRoles` (`:6085`) | Capture no-sharing schema/role evidence at staged target 008. | Exact global lists include `sharing=sharing_owner`, `sharing_owner`, and `sharing_role`; no post-final zero-sharing checks. |
| `assertFunctionsAndTrigger` (`:6854`) | Preserve earlier staged inventories. | Retain every prior global entry and append exactly three sharing functions and four qualified sharing triggers. |

The exact live fence is set-based, not count-only: three sharing tables, two sharing roles, three sharing functions, four user triggers, exact global five SECURITY DEFINER services, nine successful migration history rows/resources, and no V010 expansion. Existing evidence and all other exact checks remain binding.

## 4. Scope and validation

- Plan edits: §7.2 staged migration/helper paragraph and retained V008 inventory paragraph only. No other plan sections changed.
- Frozen product/schema scope unchanged: three sharing tables, 38 columns, 31 checks, six FKs, nine defaults, five secondary indexes, 13 physical indexes, three functions, four triggers; no DDL, ACL, schema, slice or product expansion.
- Test contract unchanged: three slices, seven planned additions plus one rename, Failsafe 57→60→62→64, and ten Surefire tests. No test was run or modified. `rg -c '@Test'` yields 58 textual hits, including the known comment match; the review-confirmed actual annotation count remains 57.
- Changed paths: `docs/gateflow/pa9-v009-sharing-foundation-plan.md` and exactly `docs/gateflow/pa9-v009-sharing-foundation-plan-fix-5.md`.
- Whitespace: scoped trailing-whitespace search on the corrected plan found no matches; final byte is LF. A repository-wide `git diff --check` attempt used the explicit metadata path but produced checkout-wide CRLF diagnostics from unrelated tracked files, so it is not claimed as a clean scoped Git check. No paths were modified by that read-only command.
- No implementation/runtime work, tests, network, mutating Git, staging, commit, push, PR, approval, or comment occurred.

No other source contradiction blocked this bounded correction. Plan-fix completion does not advance any later gate.
