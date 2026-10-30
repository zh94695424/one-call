# Plan Fix 1: PA7 V007 Activation and Consent Foundation

- Task label: `pa7-v007-planfix1-gpt56-20261029-192651`
- Gate/work unit: `fix` / `PA7-v007-activation-consent-foundation`
- Run directory: `/tmp/sub-agents.pa7-planfix.zJiiUD`
- Binding goal: `docs/gateflow/project-execution-control.md:37`; unchanged.
- Fixed target: `docs/gateflow/pa7-v007-activation-consent-foundation-plan.md`
- Review artifact: `docs/reviews/plan-review-20261029-191243.md`, SHA-256 unchanged at `3a2ecb9d448e8acd75844577ed3e046f8b5d9e9b094233a49fd141936c22832b`.
- Pre-fix plan SHA-256: `61c20afa8a32d64b0717f8be17be7bccb83aaabf0597e11ba1baa4b437b26b93`.
- Post-fix plan SHA-256: `e39eb0b680d1a349bd254b5265cb3c9d514516cef76b7ea90fcb7cb72583f9e0`.

## Accepted Findings

| Finding | Status | Bounded correction and updated plan locations |
| --- | --- | --- |
| PR-001 (medium) | `已修复` | Section 9.1 now compares the exact sorted seven-name V001-V007 migration set and asserts count 7, rejecting missing, extra and V008+ files. `EXPECTED_METHODS` accepts only 48/S1 or 51/S2 and drives the source count; section 9.3 uses the same parameter for source, CI and XML set/count checks. Both sections instruct running unchanged at each checkpoint. Locations: `§9.1` lines 627-670; `§9.3` lines 700-729. |
| PR-002 (high) | `已修复` | Section 7.1 stages a fully valid random-orphan `purchase_consent_id` customer-order insert at V004 inside a transaction and rolls it and its minimal parents back before advancing to V005. The probe is assigned to the count-neutral renamed `v004ForeignKeyAndStagedConsentFkBackstops` method (`§7.1-7.2`, lines 446-454). Final-V007 valid orders use real consent parents; the renamed V007 FK method separately requires orphan failure with SQLSTATE `23503` and exact `fk_customer_order_consent` (`§7.3`, §8.1, §9.4; lines 469, 572, 737). No committed orphan crosses a migration boundary and no method is added. |
| PR-003 (medium) | `已修复` | Sections 7.1-7.2 and 4.7 retain V004-created `activation_consent` and all prior-wave schema privileges. Staged V006 specifically asserts absence of V007 tables/functions/triggers/FK/table-level and column grants, and absence only of the new `activation_consent_role` schema USAGE grants on `subject_number`, `customer_iam` and `organization_access`. Latest asserts exactly those three grants without `CREATE` and exact declared column reads; `operations_iam` remains denied. Locations: `§4.7` line 339; `§7.1-7.2` lines 446-461. No V004 grant is revoked or challenged. |

## Scope and Decisions

- Changed paths authored by this fix: `docs/gateflow/pa7-v007-activation-consent-foundation-plan.md` and this artifact only.
- No changes to Goal Confirmation, frozen V007 inventory, two-slice boundary, exclusions, migrations, tests, CI, README, review artifact, controller, or Git state.
- Counts remain exactly 3 tables, 43 columns, 31 checks, 11 FK decisions, 6 defaults, 2 trigger functions, 3 triggers, two slices, and method evolution 45 → 48 → 51. The V001-V006 blob hashes remain the frozen plan values.
- Docs decision: no product documentation change; this is a plan-only correction. Existing bounded README synchronization remains part of the approved implementation scope.
- Blocking questions: none identified. No scope, design, schema, state-transition, or method-count decision is reopened.

## Validation

- Confirmed the review artifact SHA-256 is unchanged from the supplied value.
- Verified `git hash-object` for V001-V006 against all six frozen blob IDs in the plan; all matched.
- Confirmed the edited plan retains the exact frozen inventory, two slices and 45 → 48 → 51 method totals; the V004 probe is assigned to a renamed existing method and adds no method.
- `git diff --check` and the new-file diff check pass.
- Static shell/Python check blocks were inspected for parameter consistency and fail-closed assertions. They were not run against a completed S1/S2 implementation because V007, the evolved Java/CI evidence and matching Failsafe XML are not present at this plan-fix gate.
- Docker/Maven/container tests were not run; this bounded plan fix does not enter implementation.
- Task-start-scope check: this fix wrote only the plan and this artifact. Other controller/launcher paths visible in the worktree were pre-existing task infrastructure and were not changed.

## Residual Risks

- Actual PostgreSQL execution of the V004 staged probe and V007 FK behavior remains to be demonstrated by approved S1 implementation and its tests; classification: covered by the later approved S1 slice.
- Execution of the exact ACL prefix/latest matrices remains to be demonstrated by S1; classification: covered by the later approved S1 slice.
- Existing service-level consent semantics remain explicitly deferred outside this migration work unit to their named application-service work; classification: assigned to later work unit.
- No newly introduced or unclassified residual risk remains from this fix.

## Completion

- Fix gate status: complete; PR-001, PR-002 and PR-003 are `已修复` in the plan specification.
- No accepted finding remains unresolved and no new blocker was found.
- Next entry point: fresh independent plan re-review. This artifact does not claim re-review, plan acceptance, implementation, or any later Gateflow gate.
