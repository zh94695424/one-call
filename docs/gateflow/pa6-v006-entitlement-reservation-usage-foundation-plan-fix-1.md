# PA6 V006 Plan Fix 1

- Task label: `pa6-v006-planfix-gpt56-20261029-083342`
- Work unit: `PA6-v006-entitlement-reservation-usage-foundation`
- Gate: `fix` after failed adversarial plan review
- Scope: revise the V006 implementation plan for the four accepted findings, apply the user-authorized Option A two-sentence docs/14 erratum, and create this durable fix record only
- Run directory supplied for evidence: `/tmp/sub-agents.pa6-planfix.nTO8x1` (no agent was dispatched and no runner artifact was written)
- Changed files:
  - `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan.md`
  - `docs/14-production-data-model-and-flyway-schema-spec.md` (only the §4.30 and §4.31 control sentences)
  - `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan-fix-1.md`
- Prohibited gates/actions not entered: re-review, implementation, staging, commit, push, PR creation/mutation, merge and agent dispatch

## Finding Fix Mapping

| Finding | Fix applied | Verification target |
| --- | --- | --- |
| F-01 | Recorded user-authorized Option A as binding; removed every “non-blocking naming alignment/note” treatment; corrected only docs/14 §4.30 and §4.31; retained §4.35 unchanged; synchronized exact trigger bindings, eight-function inventory, ACL and `pg_proc`/`pg_trigger`/`tgfoid`/`proacl` test wording. | Exact inventory is 1 service + 7 trigger functions and 7 triggers; six dedicated guards receive runtime EXECUTE, shared `reject_row_mutation` does not. |
| F-02 | Added explicit `CREATE FUNCTION entitlement_metering.issue_first_trial(...)` immediately after its four dependency tables; assigned grants to `platform_reliability_owner`, `entitlement_metering_owner` and `subject_number_owner` according to actual ownership; removed the invented `catalog_order_owner` block; preserved V004 runtime grants and PUBLIC-zero boundaries. | Clean V001-V006 apply under owner role blocks; exact schema/table/column/function ACL catalog assertions. |
| F-03 | Enumerated every affected existing V003/V004/V005 assertion and fixture: staged target-005 negative proof; all zero table/function assertions; cumulative function/trigger/signature/config lists; V004/V005 schema and platform-owner ACL assertions; all three legal `ivr_decision.reservation_id` fixtures; V005 15-FK list plus separate late-FK presence; exact real `usage_reservation` parents; wording corrected to “no service function beyond `issue_first_trial`.” No prior behavior assertion may be removed or weakened. | Live V005 prefix proves function/FK absence, final V006 prefix proves presence, and exact 41/45 method sets remain green. |
| F-04 | Replaced seven-table `TRUNCATE` with method-local FK-safe `DELETE`; external `ivr_decision` references are deleted first, followed by Outbox/V006 children and parents and then older parents; no `CASCADE`, table-wide delete or cross-method deletion. Named append-only triggers are transactionally disabled only for cleanup, re-enabled and asserted before commit; runtime `DELETE`/`TRUNCATE` denial evidence remains separate. | Zero method-local rows for every touched table/key and all four named triggers restored to `tgenabled = 'O'`. |

## Option A Errata and Inventory

The controlling sources are docs/14 §10.16 and §11.4.

- §4.30 now states `trg_entitlement_grant_immutable` uses `entitlement_metering.guard_entitlement_grant()`.
- §4.31 now states `trg_trial_grant_immutable` uses `entitlement_metering.guard_trial_grant()`.
- §4.35 is unchanged: `trg_usage_fact_immutable` uses `entitlement_metering.reject_row_mutation()`.
- Service function inventory: exactly `entitlement_metering.issue_first_trial(...)`.
- Trigger function inventory: exactly `guard_trial_policy_version`, `guard_entitlement_grant`, `guard_trial_grant`, `guard_entitlement_balance`, `guard_entitlement_issuance`, `guard_usage_reservation`, `reject_row_mutation`.
- Result: exactly 8 V006 functions total (1 service + 7 trigger functions) and 7 triggers.

## Closed Implementation Ambiguities

- Forced nested-Outbox failure uses a transaction-local revoke by `platform_reliability_owner`, a savepoint-bounded function call, exact native `42501`/`permission denied for function outbox_append`, rollback proof, fresh-connection privilege restoration and subsequent success.
- A committed `GRANTED` is observed from a second connection and then removed by the explicit method-local cleanup; rollback-only probes remain rollback-only.
- Request fingerprints stay service-owned RFC 8785-compatible hashes of the exact §10.17 trial scope. V006 validates and compares the supplied 64-hex value but does not claim generic PostgreSQL RFC 8785 support.
- The one fixed first-trial Outbox payload uses an exact fixed-field canonical byte expression and an independent Java canonicalization/hash oracle; no reusable SQL serializer/canonicalizer is added.
- `first_granted_at` is captured `trial_grant.granted_at` for `GRANTED` and the persisted original `trial_grant.granted_at` for `REPLAY`/`ALREADY_GRANTED`.
- Whitespace/null/malformed argument mapping, exact `P0001` `TRIAL_*` messages, and frozen-row `TRIAL_DATA_INTEGRITY` predicates are explicit.
- Two behaviorally bounded slices remain: S1 establishes the runnable exact migration/ACL/function contract; S2 proves lifecycle/FK/rollback/concurrency behavior. A third slice would be a forbidden mechanical split of one migration/identity wave; one slice would collapse too much falsifiable behavior into one checkpoint.
- Validation is bounded and fail-closed, with exactly one final full Testcontainers run per accepted S1/S2 checkpoint.

## Validation Performed

The plan-fix author inspected the review-cited live source rather than relying on review prose alone, including the PA6 control contract, docs/14 §4.30/§4.31/§4.35/§10.9/§10.11/§10.16/§10.17/§11.4, V001 and V004 owner/error precedents, and the cited `PostgresContainerIT` global lists, zero-count assertions, ACL matrices, reservation fixtures and late-FK checks.

Commands/results:

- `git diff --check -- docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan.md docs/14-production-data-model-and-flyway-schema-spec.md docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan-fix-1.md` — exit 0.
- `git -c core.autocrlf=false diff --no-index --check /dev/null <untracked-path>` for the revised plan and this artifact — expected difference exit 1 with zero check output for each, proving no whitespace error in the untracked files.
- `git diff --numstat -- docs/14-production-data-model-and-flyway-schema-spec.md` — `2 2`; the unified diff contains only the two authorized control-sentence replacements.
- `git status --short` plus tracked-path comparison against the task-start baseline — newly changed tracked path is only the allowed docs/14 file; the pre-existing tracked control-file change remains, and the revised plan/new fix artifact are the only allowed task paths shown as untracked/modified.
- Bounded content checks for exact docs/14 errata, unchanged §4.35 binding, removal of stale naming-note treatment, exact 1+7 function inventory, owner-correct role blocks, staged V005 proof, real reservation parents, no-service-function wording, FK-safe cleanup and balanced Markdown fences — passed.
- Maven/Docker/Flyway/Testcontainers were not run because this gate is documentation-only and no implementation file changed.

## Residual Risks and Open Inputs

| Item | Status | Owner / destination |
| --- | --- | --- |
| Fixed-field Outbox canonical byte expression must match the independent Java oracle exactly. | Bounded implementation risk, not a design input; stop rather than substitute `jsonb::text` as a generic RFC 8785 claim. | S1 implementation author and S1 executable evidence. |
| Cleanup must restore all four temporarily disabled append-only triggers even on failure. | Bounded test-harness risk; cleanup transaction rollback plus post-restore catalog assertion is mandatory. | S2 implementation author and S2 cleanup evidence. |
| Q4/Q5/Q8/Q10/Q15/Q17/Q18/Q20/Q21/Q23/Q25/Q26/Q28 and OQ-5 remain deferred exactly as in the binding design. | Non-blocking for V006; no value or product policy was invented. | Named Product/Finance/Platform/Security/API/later-wave owners in the revised plan. |
| Pre-existing controller modification and historical untracked helper files remain in the workspace. | Preserved; not modified or staged by this task. | Controller/workspace owner. |

No new binding contradiction was discovered. If implementation evidence contradicts the frozen sources or the authorized adjudication, the implementation gate must stop rather than improvise.

## Completion Status

- Status: complete for the `fix` gate only; no later gate entered.
- All four accepted findings: fixed in the revised plan and mapped above.
- Durable artifact path: `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan-fix-1.md`.
