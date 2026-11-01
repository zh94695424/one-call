# PA9 V009 Sharing Persistence Foundation — Plan Fix 6

- Task label: `pa9-v009-planfix6-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`.
- Gate: `fix` — bounded owner-action correction only; author date: 2026-10-31.
- status: plan-fix-complete
- No implementation, runtime, network, mutating Git, commit, PR, approval or comment action occurred.

## 1. Frozen inputs and identity

Read the current plan, fix-1 through fix-5, the active checkpoint/control, round-5 A/B reviews, frozen docs 14/15 and the relevant source. The controller accepts the round-5 A finding: doc-14 §4.44/§7.4 and doc-15 line 145 authorize extraction-code rotation only as `LOCKED→ACTIVE`. No frozen-source contradiction was found.

| Artifact | SHA256 |
| --- | --- |
| Supplied pre-fix current plan | `392937a4fbf7d31c7d27aa17d5468c1d07334d58b04f8e996f15ae0422860530` |
| Final post-fix current plan | `85bd36a17a1b1358363bfe5f7ce276876cffa7b6374b909e2674b097875dd3a` |

## 2. Exact correction

Exact plan sections changed: §4.1 grant guard/revision matrix and structural classification; §7.1 S2 method responsibility; §7.4 lifecycle and rollback witnesses; §10 residual risks. The fix artifact is this new file. No other plan section changed.

Changed only the plan's legal/structural lifecycle wording, S2 responsibility, S2 witness list, and residual table:

- Removed `ACTIVE→ACTIVE | Extraction-code rotation` from the legal owner-action matrix.
- Retained `LOCKED→ACTIVE | Extraction-code rotation only` as the sole legal code-rotation action.
- Did not add a DDL prohibition. The frozen guard remains structurally permissive for matching fresh salt/hash plus code-generation replacement, including same-state ACTIVE.
- Explicitly classifies same-state ACTIVE salt/hash replacement with `code_generation + 1`, `revocation_generation + 1`, and `revision + 1`, all other fields unchanged, as structurally accepted but service-forbidden. It is not an owner action, does not unlock anything, and is not a future-service `CODE_ROTATED` success.
- Updated S2 method responsibility and added the rollback-isolated ACTIVE witness while preserving terminal, LOCKED non-terminal, mixed-action, and legal LOCKED→ACTIVE witnesses/negatives.
- Added the residual with owner `Sharing/Backend` and excluded owner-transition service destination.

## 3. Legal / illegal / structural matrix

| Shape | Classification | Required treatment |
| --- | --- | --- |
| `LOCKED→ACTIVE` with fresh salt/hash, `code_generation +1`, `revocation_generation +1`, `revision +1` | Legal owner action | Preserve as the sole extraction-code rotation and unlock witness. |
| `ACTIVE→ACTIVE` with fresh salt/hash and the same exact deltas | Structurally accepted; service-forbidden | Test rollback-isolated success; never list or implement it as an owner action or `CODE_ROTATED` success. |
| Same-state `EXPIRED`/`REVOKED` revocation-generation +1 and revision +1 | Structurally accepted; service-forbidden | Preserve existing witnesses and negatives. |
| Same-state `LOCKED` revocation-generation +1 and revision +1 | Structurally accepted; service-forbidden | Preserve existing witness and generation-decrease negative. |
| `ACTIVE→LOCKED` plus strict expiry increase in one UPDATE | Structurally accepted; service-forbidden | Preserve mixed-action witness; no mixed-action DDL ban. |
| Other illegal status edges, partial rotation, generation decrease, stale/nonunit revision | Structurally refused | Preserve existing exact labels and rollback expectations. |

## 4. Required ACTIVE S2 witness

Inside `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops`, use a valid `EXTRACTION_CODE` `ACTIVE` fixture with the guard enabled. Lock/read the full baseline row and admission/event counts, then:

```sql
BEGIN;
SELECT * FROM sharing.evidence_share_grant
WHERE id = :id AND status = 'ACTIVE'
FOR UPDATE;
SAVEPOINT active_rotation_structural_success;

UPDATE sharing.evidence_share_grant
SET code_salt = :fresh_code_salt,
    code_hash = :fresh_code_hash,
    code_generation = :old_code_generation + 1,
    revocation_generation = :old_revocation_generation + 1,
    revision = :old_revision + 1
WHERE id = :id
  AND status = 'ACTIVE'
  AND revision = :old_revision;
```

Expect structural success, one affected row, unchanged `ACTIVE` status, exact +1 for both generations and revision, and every other column unchanged. Classify the result service-forbidden: it is not an owner action, does not unlock anything, and must not become a `CODE_ROTATED` service success. Then `ROLLBACK TO SAVEPOINT`, assert the complete row/history baseline, `ROLLBACK`, and use a fresh connection to assert the original row, unchanged admission/event counts, and no event residue. Do not disable the guard or commit the probe. Keep the existing legal `LOCKED→ACTIVE` witness and all existing same-state terminal/non-terminal, mixed-action, and negative witnesses.

## 5. Scope and validation contract

- DDL/ACL unchanged: no table, column, CHECK, grant, trigger, function, index, slice, or test-count change; retain 3 tables, 38 columns, 31 checks, 13 physical indexes, 6 FKs, 3 functions, 4 triggers, 3 slices, Failsafe `57→60→62→64`, and Surefire 10.
- Changed paths are exactly `docs/gateflow/pa9-v009-sharing-foundation-plan.md` and this artifact.
- Pre/post plan hashes are recorded above; final plan ends with LF.
- Scoped whitespace/diff checks: inspect trailing whitespace in both allowed files; run `git diff --no-index --check /dev/null docs/gateflow/pa9-v009-sharing-foundation-plan.md` and the equivalent new-file check for this artifact. The local Windows `.git` indirection prevents a normal repository Git diff; no repository repair is authorized.
- No other path may be modified. No tests, Maven, Docker, PostgreSQL, network or implementation execution is claimed.

Residual owner: `Sharing/Backend`; excluded owner transition service destination. This fix records structural database acceptance without authorizing the service shape.

`status: plan-fix-complete`
