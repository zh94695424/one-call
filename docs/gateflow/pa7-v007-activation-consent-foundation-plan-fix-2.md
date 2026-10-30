# Plan Fix 2: PA7 V007 Activation and Consent Foundation

- Task label: `pa7-v007-planfix2-gpt56-20261029-195331`
- Gate/work unit: `fix after re-review 1 failed` / `PA7-v007-activation-consent-foundation`
- Run directory: `/tmp/sub-agents.pa7-planfix2.tDAvxr`
- Binding goal: Goal Confirmation in `docs/gateflow/project-execution-control.md:37-59`; unchanged.
- Fixed target: `docs/gateflow/pa7-v007-activation-consent-foundation-plan.md`
- Accepted finding: `PR-002` high, unresolved after re-review 1.
- Pre-fix plan SHA-256: `e39eb0b680d1a349bd254b5265cb3c9d514516cef76b7ea90fcb7cb72583f9e0`.
- Post-fix plan SHA-256: `cb2fc46f8a6dba94ffc69d935bba6e6b1a4b355c05142ef5db395583dab0682a`.

## Accepted Finding and Correction

`PR-002` identified that the revised plan assigned the V004 orphan-consent success insert to the renamed JUnit `@Test`, although the real `PostgresContainerIT` lifecycle completes `@BeforeAll` migration through latest before any test method executes. The fix changes only the lifecycle specification needed to make that accepted behavior executable:

1. The single class `@BeforeAll` migrates through target `004` and immediately invokes a dedicated staged probe helper before configuring or migrating target `005`.
2. The helper starts a transaction, creates only the minimum valid V004 parents and a fully valid `customer_order`, uses a random non-null `purchase_consent_id` with no consent row, asserts the insert succeeds, rolls back the whole transaction and verifies that the order and all probe parents are absent.
3. The helper returns an immutable structured evidence value with probe IDs and explicit insert-success/rollback-verified facts. `@BeforeAll` assigns it exactly once to a private static field; SQL, assertion or runtime failure is rethrown so setup fails immediately and cannot continue migration silently.
4. Only after successful evidence capture does setup migrate target `005`, then `006`, then latest/V007. No second container, migration downgrade, dynamic test ordering, committed orphan, disabled FK or additional `@Test` is introduced.
5. The later count-neutral `v004ForeignKeyAndStagedConsentFkBackstops` test reads the captured V004 evidence without rerunning the V004 insert, then separately proves final-V007 behavior: a valid order uses a real consent parent and a fully valid random orphan is rejected with SQLSTATE `23503` and exact constraint `fk_customer_order_consent`.

This sequence is lifecycle-correct because every V004-only database action now executes synchronously inside `@BeforeAll` while Flyway is still at target `004`; the successful immutable result is captured before any later migration, and JUnit methods only report/assert that captured prefix evidence after latest/V007 exists.

## Exact Plan Sections Changed

- `§7.1 Staged migration-prefix evidence`: replaced the impossible test-method execution assignment with the explicit `@BeforeAll` helper, transaction/rollback verification, immutable assigned-once evidence, fail-fast error propagation and exact V004 → helper/result → V005 → V006 → latest/V007 ordering.
- `§7.2 Existing method/helper evolution`: made the migration-history test assert captured evidence and made `v004ForeignKeyAndStagedConsentFkBackstops` consume that evidence plus separately verify final V007 valid-parent success and exact orphan rejection, while remaining count-neutral.
- `§7.3 Existing fixture evolution caused by the late FK`: clarified that the V004 behavior is produced by the staged setup helper and only asserted later by the renamed test.
- No other plan section was changed.

## Frozen Inputs and Validation

- Verified the exact supplied pre-fix plan SHA-256 before editing: `e39eb0b680d1a349bd254b5265cb3c9d514516cef76b7ea90fcb7cb72583f9e0`.
- Verified frozen review/fix artifacts remain byte-identical:
  - `docs/reviews/plan-review-20261029-191243.md`: `3a2ecb9d448e8acd75844577ed3e046f8b5d9e9b094233a49fd141936c22832b`.
  - `docs/gateflow/pa7-v007-activation-consent-foundation-plan-fix-1.md`: `903ea6ab30c6ec9721cfcc56ec3e960f0597e012298d4f92525330305a412ca7`.
  - `docs/reviews/plan-review-20261029-194612.md`: `385f1a3189748a05965bdafde66bbb93c8012999aed358f51501637cf9b07dc1`.
- Read the current `PostgresContainerIT.startContainerAndMigrate()` `@BeforeAll` and confirmed it performs staged migration and then latest before JUnit invokes `@Test` methods.
- Verified all frozen V001-V006 Git blob IDs still match the plan: `d7e2e6879701a5db6517434cbb9b22d5c8f2d707`, `562302e19e8d7b84399ed3dff44eab00e411cfae`, `4c274b53c7ff75416e16e7ae8dd7d0d9973170b7`, `3197138f7f7b5740f2bd0b6a4cd7b0cbd76b2cf7`, `f0d318199e33801a1cefbab87db4def07cd81dc0` and `ea2114edfd663344389a914696af19350c23a939`.
- Verified the post-fix plan contains the explicit helper invocation immediately after target `004`, assigned-once immutable evidence, fail-fast propagation, V005/V006/latest ordering and later renamed-test assertions.
- Verified only the plan and this fix-2 artifact were written by this task; pre-existing controller, launcher, prompt and prior artifact paths were not modified.
- `git diff --check` passes for both allowed paths. Docker, Maven and container tests were not run because this is bounded plan-only work.

## Changed Paths

- Modified: `docs/gateflow/pa7-v007-activation-consent-foundation-plan.md`
- Added: `docs/gateflow/pa7-v007-activation-consent-foundation-plan-fix-2.md`

No code, tests, CI, README, design source, controller or review artifact was edited.

## Preserved Contracts

- PR-001 exact V001-V007 migration-set/count and 48/51 source-CI-XML method synchronization remain unchanged.
- PR-003 prior-wave schema-grant preservation and V007-only grant absence/presence distinctions remain unchanged.
- Goal Confirmation, frozen V007 inventory/counts, two behavioral slices, 45 → 48 → 51 method totals, six additions, two count-neutral renames, scope/non-goals and completion commands remain unchanged.
- V001-V006 migrations remain immutable; no V008+, application service, product behavior, migration downgrade or extra test method enters the plan.

## Blocking Questions and Residual Risks

- Blocking questions: none. The correction is a test-lifecycle specification change and requires no product, schema or architecture decision.
- Actual PostgreSQL execution of the staged V004 probe/rollback and final V007 FK behavior remains implementation evidence for approved S1 and its independent review.
- Fixture construction, rollback verification and assigned-once evidence wiring can still be implemented incorrectly; S1 must follow the fail-fast sequence exactly and run the planned PostgreSQL 16 suite.
- Previously classified service semantics, production role provisioning/runtime switching and V008/V009 work remain deferred to their existing destinations; this fix does not reopen them.

## Completion

- PR-002 status in the plan specification: `已修复`; the V004 proof is now lifecycle-executable and the later test consumes captured evidence.
- Fix gate status: complete for this bounded plan correction.
- Completion status: `complete`.
- Next entry point, if authorized by the controller: fresh independent re-review. This artifact does not enter re-review or implementation.
