# PA6 V006 aggregate deepreview fix-1

Work unit: `pa6-v006-entitlement-reservation-usage-foundation`
Gate: `fix` (post aggregate deepreview)
Author model: MiMo-V2.6-Flash
Date: 2026-10-29
Baseline (post S2 fix-1, pre aggregate fix-1): aggregate-entry HEAD `43c0cd1c7122f194662e9a318dc90afb20a1127a`; accepted S2 checkpoint `349c1b6`; `PostgresContainerIT.java` `2f65f3f1a95c41baf68d4ad54438aa6a429ffa619546b2c2598b49b0a9c32f8a`; `backend/README.md` `c98063cf6cc98ff6193fd2ed6be3cae50c52a3be95d46973932ba699b2427cc4`; plan `7e3d09f3563feefd3e432c83d3ababfac603eafc5b6895e08907a989136745e4`.

This artifact records the bounded aggregate fix-1 for the seven controller-accepted aggregate deepreview findings A-01…A-04 / B-01…B-03. No product SQL, migration, CI workflow, application code or method-count change is included.

## Allowed changed paths

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
2. `backend/README.md`
3. `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan.md`
4. `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-aggregate-fix-1.md` (this file)

## Findings and fixes

### A-01 — category-2 savepoint evidence now observes a real committed trial row

`assertV006CategoryTwoSavepointPreservesCommittedTrial` previously committed only grant + balance and asserted their survival; the trial row was not present, so the savepoint evidence did not actually observe a surviving `trial_grant`. The method now:

- creates a method-local `trialId` registered in `V006S2LocalRows.trialGrantIds` (cleanup registration preserved);
- inserts a real `entitlement_metering.trial_grant` linked to the committed grant/balance in the commit phase (subject, `FIRST-TRIAL`, policy, grant id, unique action domain, fingerprint, persisted `granted_at`/`expires_at`);
- after the failed reservation savepoint, asserts grant, balance **and** `trial_grant` all still have exactly one row.

No claim is weakened; the savepoint rollback boundary is unchanged.

### A-02 — nested-Outbox evidence strengthened (payload identity, full rollback check, exact committed observation)

`runV006NestedOutboxAtomicityAndReplayRollbackEvidence`:

- **Part A (forced nested failure):** the zero-Outbox check no longer joins through the rolled-back trial row. It now asserts zero `platform.outbox_event` rows carrying this subject's payload identity (`payload_redacted->>'subjectId'`) and zero carrying this policy's payload identity (`payload_redacted->>'trialPolicyVersionId'`) for domain `ACTIVATION` / type `FIRST_TRIAL_GRANTED`.
- **Part B (whole-transaction rollback):** added the same payload-identity zero-Outbox assertion (`whole-transaction failure after trial writes leaves no Outbox row`), which was previously missing on this path.
- **Part C (committed observation):** replaced the 64-hex-shape-only check with exact frozen Outbox identity (`event_domain=ACTIVATION`, `event_type=FIRST_TRIAL_GRANTED`, `aggregate_type=TRIAL_GRANT`, `aggregate_id == trialGrantId`), exact payload field set (`containsExactlyInAnyOrderElementsOf(V006_OUTBOX_CANONICAL_KEYS)`), exact token values for campaignKey/ids/grantedUnits, persisted timestamp tokens equal to `"\"" + v006FormatUtcMicros(...) + "\""` for `validFrom`/`expiresAt`/`grantedAt`, and exact `v006CanonicalOutboxSha256(payload) == payload_sha256` (reusing the existing S1 Java canonicalizer oracle).

### A-03 — SQL NULL probes for the three remaining non-null arguments

`assertV006IssueFirstTrialSemantics` gains three savepoint-bounded probes, each expecting exact `P0001` / `TRIAL_ARGUMENT_INVALID` and zero trial/grant/balance/Outbox rows:

- `p_campaign_key = NULL`
- `p_action_domain = NULL`
- `p_action_event_id = NULL`

### A-04 — stale inventory wording corrected

- Class Javadoc: `Exactly 41 container methods` → `Exactly 45 container methods`.
- `backend/README.md` S3 line: removed the stale claim that `issue_first_trial` 不存在; replaced with presence as `SECURITY DEFINER` service function after V006, while preserving the staged V005-prefix absence evidence proven by the migration method (`staged V005 prefix has no issue_first_trial`, PostgresContainerIT lines 891/932).

### B-01 — V004/V005 concurrent-fixture cleanup: TRUNCATE → scoped FK-safe

Both legacy seven-table / six-table `TRUNCATE` cleanup bodies are replaced per plan §7.3:

**`cleanupV004ConcurrentFixtures(accountId, organizationId, subjectId, employeeId)`**

- Collects method-local child ids rooted only at the method's parent ids: `catalog_product` (via `catalog_version.created_by_employee_id`), `catalog_version`, `catalog_offer` (via those versions), `customer_order` (`account_id`), `payment_order` (via those orders), `payment_event` (via those payment orders). No benefit-line handling (V004 races create none; plan skips untouched tables).
- In one transaction: disables exactly the two named append-only DELETE-blocking triggers (`trg_payment_event_immutable`, `trg_catalog_product_immutable`), deletes per-id in dependency order (payment_event → payment_order → customer_order → catalog_offer → catalog_version → catalog_product), deletes parent rows (subject / organization / employee / account), re-enables both triggers, asserts `tgenabled='O'` for both, commits.
- On any error: rollback (suppressed rollback failure preserved) and rethrow — cleanup fails visibly rather than masking.
- Post-commit verify: per-id zero counts for all six child lists plus zero for the four parent ids.
- No `TRUNCATE`, `CASCADE`, unscoped `DELETE`, `session_replication_role` or global trigger disable anywhere.

**`cleanupV005ConcurrentFixtures(accountId, organizationId, subjectId, bindingId)`**

- Collects method-local ids rooted at `subject_id` / `caller_binding_id`: `ivr_decision`/`call_event`/`call_leg` (via `call_record.subject_id`), `call_record`, `inbound_session`, `outbound_call_task`. No entitlement_metering deletes (V005 races commit none).
- The `outbound_call_task.claimed_call_id → call_record(call_id)` / `call_record.task_id → outbound_call_task(id)` pair is a cyclic FK. Per plan §7.3 ("Cyclic V005 task/call fixtures … have their method-local nullable link cleared before deleting both parents"), the cleanup transactionally clears method-local `claimed_call_id` (with the named UPDATE guard `trg_outbound_call_task_guard` temporarily disabled) before deleting both parents.
- In one transaction: disables exactly `ivr.trg_ivr_decision_immutable` and (for the link clear) `trg_outbound_call_task_guard`, clears `claimed_call_id` on method-local tasks, deletes per-id in dependency order (ivr_decision → call_event → call_leg → call_record → inbound_session → outbound_call_task), deletes binding/subject/organization/account, re-enables both named triggers, asserts `tgenabled='O'` for both, commits; rollback-rethrow with suppressed.
- Post-commit verify: per-id zero counts for all six child lists plus zero for the four parent ids.

V004/V005 test behavior and late V006 FK legality are preserved: deletes are scoped to method-local ids so no cross-method rows are touched, and per-id DELETE (not TRUNCATE) remains legal under the V006 late FKs.

### B-02 — lifecycle-rule ownership clarified (docs/tests only)

- Plan §5.1: `entitlement_issuance` `PENDING → ISSUED` terminal and `usage_reservation` settle-once are marked as **future application-service lifecycle rules**; V006 enforces only row/state shapes (`ck_issuance_state_shape`, `ck_usage_reservation_state_shape`, `ck_usage_reservation_settled`), immutable identities and uniqueness.
- `PostgresContainerIT` comments in `runV006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops` corrected: the `ck_issuance_state_shape` probe is a row-shape check, not a DB-owned legal-transition freeze.
- `backend/README.md` S2 wording: `issuance 状态形状与终态 ISSUED` → shape-only wording naming the future application-service ownership.
- No V006 DDL change, no new DB transition guard, no V007, no reverse-transition characterization test.

### B-03 — orphan-subject native FK backstop probe + plan clarification

- `assertV006IssueFirstTrialSemantics` gains a savepoint-isolated orphan-subject `issue_first_trial` call with otherwise valid policy/arguments; asserts native SQLSTATE `23503` with message containing `fk_entitlement_grant_subject`, then rolls back to the savepoint and asserts zero trial/grant/balance/Outbox effects. The function is not altered; the accepted native FK backstop is preserved.
- Plan §6.2 Arguments/Order clarified: null/shape/campaign validated before the subject lock; subject existence enforced at first grant write by the existing native FK because the SECURITY DEFINER owner intentionally has no cross-module subject SELECT privilege; `TRIAL_POLICY_NOT_FOUND` may precede that FK when both policy and subject are invalid. docs/14's persisted-grant-references-existing-subject requirement is unchanged.

## Implementation constraints honored

- No new `@Test` method (still exactly 45).
- No migration, application, CI, or method-list change.
- No unscoped cleanup; no new lifecycle rule; no product-policy decision.
- Allowed writes only, as listed above.

## Validation summary

- Test-source compile (`-Pcontainer-smoke test-compile`): **BUILD SUCCESS**.
- Source `@Test` count: **45** (annotation lines; the only other `@Test` occurrence is a comment at line 2080).
- First full container run attempt failed on `concurrentV005TaskClaimAndActiveCallHasOneWinner` with a cyclic-FK `23503` from `fk_outbound_task_claimed_call` (deleting `call_record` while a method-local task still held `claimed_call_id`). Root cause: the V005 `claimed_call_id`/`task_id` cycle requires the nullable link to be cleared first (plan §7.3). A second attempt nulled only `claimed_call_id` and hit `ck_outbound_task_claimed_shape` (`claimed_call_id`/`claimed_at` must be null together). Fixed by nulling both columns under a transactional disable of `trg_outbound_call_task_guard` before the dependency-order deletes. The failed attempts are not counted as the final checkpoint.
- Final full container run (`-Pcontainer-smoke verify`): **PostgreSQL 16.15**, Failsafe **Tests run: 45, Failures: 0, Errors: 0, Skipped: 0**, BUILD SUCCESS.
- 45==45==45 test-name set equality: source `@Test` set == Failsafe XML testcase set == CI expected set from `.github/workflows/ci.yml` (all size 45, no duplicates, exact set equality).
- Default `./mvnw -B -ntp clean verify`: **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0**, BUILD SUCCESS (Surefire).
- Boot smoke: `Started OneCallApplication` present in `target/boot.log`. Fat-jar firewall: no `spring-security`/`flyway`/`jooq`/`HikariCP`/`postgresql`/`testcontainers` under `BOOT-INF/lib/`.
- `git diff --check` on allowed paths: exit 0 (LF→CRLF warnings are pre-existing WSL working-tree line-ending notices, not whitespace errors).

### Post-edit SHA-256

| File | SHA-256 |
| --- | --- |
| V001 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| V002 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| V003 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| V004 | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| V005 | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| V006 | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` (frozen, unchanged) |
| `PostgresContainerIT.java` | `a47a5ba6a4bbf724a8d71a21e1fe60cdbbba398a4183a3285b8a23bc092c3be3` |
| `backend/README.md` | `dccc05a55255c3ca75a8938666770bba1914627bc585b8d6198b8b8a7352e0f9` |
| plan | `db81ffc04170f75ff2170e5ee67a07cba8917e0e46d3837b975aba006ad09429` |
| this artifact | (self-hash computed post-write; reported in the fix hand-off, not embedded here) |

V001–V006 migrations and CI are byte-frozen and unchanged.

## Residual risks (unchanged unless noted)

- Latent V006 `taskIds` verify-loop naming from S2 fix-1 remains (behavioral coverage complete; name-only residual).
- `commitV005RaceParents` method name still implies broader parent scope than its current body (name residual only).
- `ck_usage_reservation_status` and V004/V005 TRUNCATE co-truncation residual risks from the initial review are **addressed** for the concurrent-fixture cleanup paths by B-01 (scoped per-id cleanup); other historical TRUNCATE denial probes are intentional ACL/denial assertions, not fixture cleanup.
- Independent re-review by both Reviewer A and Reviewer B, followed by controller adjudication, remains pending; this artifact does not close any finding.

## Stop line

Author stops here. No re-review, no later gate, no commit/stage/push/PR/merge/approve/dispatch.
