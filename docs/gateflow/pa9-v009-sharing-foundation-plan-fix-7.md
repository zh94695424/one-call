# PA9 V009 Sharing Persistence Foundation — Plan Fix 7

- Task label: `pa9-v009-planfix7-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`.
- Gate: `fix` — bounded live-fence assertion-superset clarification only; author date: 2026-10-31, Asia/Shanghai.
- status: plan-fix-complete
- Completion is plan-author evidence only, not independent acceptance or implementation authorization. Fresh final A/B confirmation remains required.

## 1. Inputs and hashes

Read the current plan, fix-1 through fix-6, active checkpoint/control, latest A/B round-6 reviews and actual `PostgresContainerIT.java` helper bodies/constants/call sites. The controller labels this review round 6; the review artifacts retain their historical task labels. Apply only B's accepted `needs-evidence` finding: replacing final calls must not discard non-sharing assertions unique to `assertNoV008PlusObjects`. A raised no new material finding. No unrelated cleanup is included.

| Input / output | SHA256 |
| --- | --- |
| Supplied and verified pre-fix current plan | `85bd36a17a1b1358363bfe5f7ce276876c6ffa7b6374b909e2674b097875dd3a` |
| Post-fix current plan | `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57` |
| Unchanged source `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `82ba4ede3184b1506e90f64c077da95a3ccaf1eee1daa0c9db3c61ada77fe8bf` |
| A round-6 `docs/reviews/plan-review-20261031-092149.md` | `6d9b5398b503de8bc0409b4555bce0748f6ae8b12ad870f39b69b6fd988176d9` |
| B round-6 `docs/reviews/plan-review-20261031-092916.md` | `f21d0ddfcf8a898a49d0b2bd5806613c7b2e34fbc7876d281a31167f06d86b84` |

## 2. Bounded resolution

Only §7.2 changes: insert an executable assertion contract naming the planned final helper `assertV009LiveFence(Connection)`. All three final replacement calls (`:2459`, `:2512`, `:2595`) must execute that helper's full assertion list against the final V009 database. The list is an equivalence/superset of `assertNoV008PlusObjects`: retain every non-sharing assertion, strengthen count-only inventories to exact identities, and flip only sharing absence to exact sharing presence. Retaining checks only at target 008 does not satisfy the final contract.

The helper name is a plan obligation, not an implemented Java method. Source line anchors refer to the unchanged current source and do not authorize an implementation edit in this gate.

## 3. Assertion-preservation matrix

Every final row below executes inside `assertV009LiveFence` at each of the three replacement call sites. Exact literal sets are included in the §7.2 diff in §5, not inferred from totals.

| Source assertion / anchor | Staged target 008 | Final V009 / preservation obligation |
| --- | --- | --- |
| `assertNoV008PlusObjects` IVR set, `:8262` onward | Retain exact V005 six-table set. | Retain the same six named tables, no extras. |
| V006 set; constant `V006_TABLES` at `:7775` | Retain exact seven entitlement tables. | Retain all seven names by exact set equality. |
| V007 set; constant `V007_TABLES` at `:8082` | Retain exact three activation-consent tables. | Retain all three names by exact set equality. |
| V008 set; constant `V008_TABLES` at `:168` | Retain exact five evidence tables. | Retain all five names by exact set equality. |
| Sharing namespace/roles absence in both absence helpers (`:8262`, `:23962`) | Execute genuine no-sharing schema/roles assertions from both helpers before V009. | Flip to exact `sharing=sharing_owner` and `sharing_owner`/`sharing_role` in complete global schema/role inventories. |
| Evidence-role count in `assertNoV008PlusObjects` | Retain `evidence_owner`/`evidence_role` presence. | Require exactly those two evidence roles by identity in the complete exact global role inventory, not a count alone. |
| Entitlement SECURITY DEFINER list, `:8305` | Retain `issue_first_trial` only. | Retain exact singleton `entitlement_metering.issue_first_trial`; no extra entitlement SECURITY DEFINER. |
| Global SECURITY DEFINER count, `:8312`; also `assertNoV009PlusObjects` | Retain the existing five-service wave. | Compare exact five qualified services: `audit.append_fact`, `entitlement_metering.issue_first_trial`, `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append`; exclude extra SECURITY DEFINER functions across all user schemas. |
| Future-service absence, `:8314` onward | Retain all six unique forbidden names. | Retain absence across `pg_proc` of `select_call_action`, `record_activation_next_action`, `reserve_call_usage`, `settle_call_usage`, `publish_trial_policy`, `disable_trial_policy`. The repeated source literal for `record_activation_next_action` does not create a seventh identity. |
| Legacy IVR absence, `:8321` | Retain `ivr.call_task` and `ivr.call_session` absence. | Retain both final absences. |
| Late consent FK, `:8323` onward | Retain `catalog_order.fk_customer_order_consent` presence. | Retain the FK identity on `catalog_order.customer_order`, not a generic FK total. |
| Cross-schema sharing-name exclusions, `:8331` onward | All three names absent from `activation_consent`, `entitlement_metering`, `evidence`, and `sharing`; retain the genuine sharing-table absence from both absence helpers. | All three names still absent from the first three schemas, but present exactly as the complete three-table set in `sharing`. No misplaced-name exclusion is dropped. |
| Planned live sharing/global inventories | Sharing objects genuinely absent; earlier staged inventories retained. | Exact three sharing tables, two roles, three local functions, four qualified user triggers, complete prior global inventories plus sharing, nine migration resources/history rows and no V010/extra sharing object. No count-only substitute or invented schema denylist. |

S1 acceptance must compare the replacement helper's executed assertion list row by row with the actual `assertNoV008PlusObjects` body, not merely compare helper names or totals. This is a future implementation verification obligation, not runtime evidence supplied by this document.

## 4. Staged / final routing retained

| Helper / call | Target 008 | Final V009 |
| --- | --- | --- |
| `assertNoV008PlusObjects`; final V006/V007 calls `:2459`, `:2512` | Preserve genuine sharing schema/roles/tables absence and every non-sharing check. | Do not run staged absence helper; replace both calls with `assertV009LiveFence` executing the full matrix. |
| `assertNoV009PlusObjects`; final V008 call `:2595` | Preserve genuine sharing schema/roles/tables absence and five-service checks. | Do not run staged absence helper; replace call with the same full `assertV009LiveFence`. |
| `assertRiskOpenClearReopenRevisionProtocol`; direct absence `:18125` | Capture genuine sharing-schema absence before target 009. | Existing fix-5 routing unchanged: remove/replace only that direct absence; preserve every non-sharing risk-transition assertion and behavior. |
| `assertV006PresenceAndLateFk`, `:8343`; call `:2399` | No sharing-specific edit. | No sharing-specific edit; actual helper has no sharing-absence assertion. |

## 5. Exact §7.2 diff

The complete plan diff is this single insertion; all existing plan lines, including prior routing/risk-helper wording, remain byte-identical.

```diff
--- a/docs/gateflow/pa9-v009-sharing-foundation-plan.md
+++ b/docs/gateflow/pa9-v009-sharing-foundation-plan.md
@@ -335,0 +336,13 @@
+Implement that final live fence as `assertV009LiveFence(Connection)`, invoked at all three replacement call sites (`:2459`, `:2512`, `:2595`). Its executed assertions must be an equivalence/superset of `assertNoV008PlusObjects`: no non-sharing assertion may be silently removed or relegated only to staged target 008. Only sharing-absence assertions flip to exact sharing presence at final V009. The helper must explicitly execute these checks against the live final database:
+
+- Exact V005 `ivr` six-table set: `call_event`, `call_leg`, `call_record`, `inbound_session`, `ivr_decision`, `outbound_call_task`.
+- Exact V006 `entitlement_metering` seven-table set: `entitlement_balance`, `entitlement_grant`, `entitlement_issuance`, `trial_grant`, `trial_policy_version`, `usage_fact`, `usage_reservation`.
+- Exact V007 `activation_consent` three-table set: `consent_record`, `service_activation`, `service_activation_transition`.
+- Exact V008 `evidence` five-table set: `evidence_artifact`, `evidence_event`, `evidence_record`, `media_object`, `recording`; exactly the two evidence roles `evidence_owner` and `evidence_role`, retained in the complete exact global role inventory.
+- `entitlement_metering.issue_first_trial` as the only entitlement_metering SECURITY DEFINER function; exactly the five global user SECURITY DEFINER services `audit.append_fact`, `entitlement_metering.issue_first_trial`, `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append`, with no extra SECURITY DEFINER function in any user schema.
+- Absence of every future-service name `select_call_action`, `record_activation_next_action`, `reserve_call_usage`, `settle_call_usage`, `publish_trial_policy`, `disable_trial_policy` across `pg_proc`; absence of legacy `ivr.call_task` and `ivr.call_session`; presence of `catalog_order.fk_customer_order_consent` on `catalog_order.customer_order`.
+- Each of `evidence_share_grant`, `share_admission`, `share_access_event` absent from `activation_consent`, `entitlement_metering` and `evidence`, but present exactly in `sharing` as its complete three-table set. These misplaced-name exclusions remain final assertions, not staged-only evidence.
+- Exact sharing schema/owner `sharing=sharing_owner`, two roles `sharing_owner`/`sharing_role`, three local functions `sharing.guard_share_grant`, `sharing.guard_share_admission`, `sharing.reject_row_mutation`, and four qualified user triggers `sharing.evidence_share_grant.trg_share_grant_guard`, `sharing.share_admission.trg_share_admission_guard`, `sharing.share_admission.trg_share_admission_immutable`, `sharing.share_access_event.trg_share_access_event_immutable`; no V010 or extra sharing object. Preserve complete prior global schema/role/function/trigger inventories and exact nine migration resources/history rows; compare exact sets/identities, not counts alone.
+
+At staged target 008, retain the genuine sharing schema/roles/tables absence from both `assertNoV008PlusObjects` and `assertNoV009PlusObjects`, together with their non-sharing assertions. Neither staged absence helper runs at final V009. Before accepting S1, extract the executed assertion list of `assertV009LiveFence` at each of the three final call sites and compare it row by row with `assertNoV008PlusObjects`: every non-sharing assertion must still execute, sharing absence must become exact sharing presence, and the no-V010/exact global inventories must remain. This clarifies assertion preservation only; the risk-helper handling and `assertV006PresenceAndLateFk` no-sharing-edit rule above are unchanged, with no DDL, ACL, table, service, slice or test-count change.
+
```

## 6. Changed paths, validation and unchanged scope

Task-owned changed paths are exactly:

1. Modified `docs/gateflow/pa9-v009-sharing-foundation-plan.md`.
2. Created `docs/gateflow/pa9-v009-sharing-foundation-plan-fix-7.md` (this artifact).

Validation performed using read-only local inspection and in-memory comparisons; no temporary baseline file was written:

- Reconstruct pre-fix plan by removing only the new §7.2 insertion. Its SHA256 equals the supplied pre-fix hash. All bytes before §7.2 and from §7.3 onward compare equal; the exact unified diff above is reproducible with Python `difflib.unified_diff` and has one insertion hunk only.
- Static extraction compares the exact V005 six-table literal list and V006/V007/V008 constants from actual Java source with the new plan's literal table sets, not merely their lengths. All four exact sets match; all unique future-service names, evidence roles, legacy names, sharing names and late-FK name are explicitly mapped. This validates documentation against source, not an implemented fence.
- Scoped no-index whitespace checks use `git -C /tmp -c core.autocrlf=false diff --no-index --check /dev/null <absolute-task-path>` for both allowed documents, avoiding the broken worktree Git pointer without repair. Each returns 1 for new-file differences and no stdout/stderr diagnostics. Independent trailing-whitespace and final-LF checks pass on both files. Ordinary repository diff is not claimed as clean or repaired.
- Before/after sorted path/content SHA256 manifests exclude only the two allowed paths and cover 1,032 other files. Both manifest hashes are `b32f6d2fc2acd5007ed8f54c875086ba1f43fa7bb7716365667e8d4bff77714a`. This preserves pre-existing controller dirt, reviews, prior fixes, source, migrations, CI and all other file content; no whole-worktree-clean claim is made.
- Final plan SHA256 is the post-fix hash in §1. Artifact SHA256 is reported externally after writing, not embedded circularly.

Unchanged scope: no implementation or runtime action; no DDL, ACL, table, column, service, constraint, index, trigger, slice or test-count change. Retain three sharing tables, 38 columns, 31 checks, six FKs, nine defaults, five secondary/13 physical indexes, three local functions/four triggers, three slices, seven planned test additions plus one rename, Failsafe `57→60→62→64` and Surefire 10. V001–V008 bytes and every previously corrected lifecycle/witness/residual contract remain unchanged. No tests, Maven, Docker, PostgreSQL, network, mutating Git, commit, PR, approval or external comment action occurred.

B's accepted clarification is resolved at the plan-contract level only. No later gate is entered and no independent review outcome is claimed.

`status: plan-fix-complete`
