# PA8 V008 S1 Completion 1 — Bounded Corrections

## 1. Task identity and verdict

| Field | Value |
| --- | --- |
| Gateflow work unit | `PA8-v008-evidence-foundation` |
| Task label | `pa8-s1-completion-1` (controller-driven repair of blocked S1) |
| Provider / runtime | mimo / Claude Code agent session (`mimo-v2.6-flash`) |
| Slice | S1 only (catalog / security / ACL / FK / staged-prefix) |
| Base implementation | `docs/gateflow/pa8-v008-evidence-foundation-s1-implementation.md` (blocked) |
| Binding plan | `docs/gateflow/pa8-v008-evidence-foundation-plan.md` |
| Controller findings repaired | 6 (compile syntax, digest canonicalization, expectCommitFailure isolation, reservation fixture columns, snapshot JSON shape, cleanup completeness) |
| **Verdict** | **`corrections applied; runtime still controller-owned`** — all six controller findings corrected in source; full test suite not executed in this session; controller must run compile + focused + full verification |

## 2. Corrections applied

### Finding 1 — V008_CHECK_DEF_MAP missing close parentheses (JDK21 test-compile failure)

**Defect:** Five `map.put(...)` calls in the `V008_CHECK_DEF_MAP` static initializer ended with `)));` instead of `))));`, omitting the closing parenthesis for `map.put` itself. Affected lines: 23748 (media_object), 23771 (recording), 23798 (evidence_record), 23801 (evidence_artifact), 23812 (evidence_event).

**Fix:** Each of the five `map.put` statements now closes with `))));`. Parenthesis balance verified for each table block.

### Finding 2 — Migration digest witness used CRLF digests with permissive LF fallback

**Defect:** `assertV001ThroughV007MigrationDigestsUnchanged` pinned the plan §6.1 digests (which are raw CRLF file bytes) and had an LF-normalization fallback that compared the same CRLF digests. The fallback was logically dead and the digests were not the canonical Git blob hashes.

**Fix:** Replaced with pinned canonical LF (Git blob) digests from the controller:

| Migration | Canonical LF SHA-256 |
| --- | --- |
| V001 | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| V002 | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| V003 | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| V004 | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| V005 | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| V006 | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| V007 | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |

The assertion now always normalizes CRLF→LF via `toLfBytes` before hashing. No migration file was modified.

### Finding 3 — expectCommitFailure caught setup SQLException as commit failure

**Defect:** The `setup.run(connection)` call was inside the try block that catches `SQLException` and asserts commit-failure SQLSTATE. A setup-time SQL error (e.g., constraint violation during fixture insert) was misclassified as a commit failure.

**Fix:** `setup.run(connection)` now executes outside the try-catch. Setup errors propagate to the caller. The try-catch only wraps `connection.commit()`. The `finally` rollback is retained.

### Finding 4 — Reservation fixtures used non-existent columns

**Defect:** `v008InsertReservationSameTx` and `v008CommitReservation` inserted `usage_reservation` with columns `entitlement_type`, `units`, `reserved_at` that do not exist in V006. The `trial_policy_version` insert used `created_by_employee_id`, `granted_units`, `validity_days` instead of `published_by_employee_id`, `granted_seconds`, `valid_days`. The `entitlement_grant` insert used non-existent `policy_version_id`. The `entitlement_balance` insert used non-existent `subject_id`, `entitlement_type`, `used_units`, `available_units`. The `catalog_order` chain used wrong columns and was unnecessary for the reservation prerequisite chain.

**Fix:** Both methods now insert only the actually-needed prerequisites using verified V006 columns:

- `trial_policy_version`: `(id, campaign_key, version_no, granted_seconds, valid_days, snapshot, snapshot_sha256, published_by_employee_id, published_at)`
- `entitlement_grant`: `(id, subject_id, source_type, source_ref_id, entitlement_type, granted_units, valid_from, expires_at, snapshot, snapshot_sha256)` with `source_type='TRIAL'`
- `entitlement_balance`: `(id, grant_id, granted_units)`
- `usage_reservation`: `(id, call_id, balance_id, subject_id, authorized_units, authorization_expires_at)`

The unnecessary `catalog_order` / `customer_iam` / `organization_access` chain was removed. Both methods insert as `entitlement_metering_owner` for entitlement tables.

### Finding 5 — Snapshot JSON placed sourceEventDomain/sourceEventId inside source

**Defect:** Five evidence_record INSERT fixtures built the snapshot JSON with `sourceEventDomain` and `sourceEventId` nested inside the `source` subobject. The migration CHECK `ck_evidence_snapshot_shape` requires these at the top level of the snapshot (`snapshot->>'sourceEventDomain'`, `snapshot->>'sourceEventId'`), while `callId`/`sessionId`/`subjectId`/`reservationId`/`recordingId` live inside `source`.

**Fix:** All five fixtures now build:

```json
{
  "capture": {},
  "content": {},
  "sourceEventDomain": "EVIDENCE",
  "sourceEventId": "...",
  "source": {
    "callId": "...",
    "sessionId": "...",
    "subjectId": "...",
    "reservationId": "...",
    "recordingId": "..."
  }
}
```

Affected locations: missing-subject probe, missing-session probe, missing-reservation probe, `v008InsertRecordOnlySameTx`, `v008InsertValidRecordAndCreationEvent`.

### Finding 6 — v008CommitReservation cleanup incomplete

**Defect:** `v008CommitReservation` committed employee → policy → grant → balance → reservation but returned only `reservationId`. The cleanup helper deleted only `usage_reservation`, leaving committed prerequisite rows (balance, grant, policy, employee) as residue.

**Fix:**

- Added `V008ReservationFixture` record carrying `reservationId`, `policyId`, `grantId`, `balanceId`, `employeeId`.
- `v008CommitReservation` now returns `V008ReservationFixture`.
- `cleanupV008LocalRows` signature changed from `List<UUID> reservationIds` to `V008ReservationFixture reservationFixture`.
- Cleanup now deletes in FK-safe order: reservation → balance → grant → policy → employee → session/call → subject.
- Fresh-connection per-ID zero checks added for all five fixture IDs.
- Call site updated to use `cleanupReservation.reservationId()` for the record insert and pass the fixture to cleanup.

## 3. Files changed

| Path | Action | Note |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modify | All six corrections applied |

No other files were modified. V008 migration unchanged. CI expected-set unchanged (54). README unchanged.

## 4. Scope fence preserved

- Still S1 only (3 new methods + rename = 54 total).
- No S2 methods added.
- No relaxed constraints, no SQLSTATE unions, no check drops.
- No commits/push/PR/gate transitions.
- No V009 objects, no application services.

## 5. Validation status

| Check | Status |
| --- | --- |
| Parenthesis balance in V008_CHECK_DEF_MAP | Corrected; static verification only |
| Digest canonicalization | Corrected to pinned LF digests with mandatory normalization |
| expectCommitFailure isolation | Setup outside catch; finally rollback retained |
| Reservation fixture columns | Aligned to actual V006 schema |
| Snapshot JSON shape | Aligned to migration CHECK definition |
| Cleanup completeness | All prerequisites cleaned with per-ID zero checks |
| JDK21 test-compile | **Not executed** — controller owns verification |
| Focused container tests | **Not executed** — controller owns verification |
| Full container suite (54) | **Not executed** — controller owns verification |

## 6. Remaining unverified assertions

The following were corrected statically but require controller-executed runtime verification:

1. `V008_CHECK_DEF_MAP` whole-def literals against live PG16 `pg_get_constraintdef` output (first live run may require observational one-shot correction per prior artifact §4.4).
2. Digest assertion against actual classpath bytes (LF normalization of CRLF working-tree files).
3. Reservation fixture inserts against live V006 schema.
4. Snapshot JSON shape against live CHECK enforcement.
5. Deferred parent-delete commit failure and pair survival observation.
6. Complete cleanup with per-ID zero residue on fresh connections.

## 7. Completion status

1. **Verdict:** corrections applied; runtime verification controller-owned
2. **Artifact paths:** this file + corrected `PostgresContainerIT.java`
3. **Exact files changed:** `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
4. **Validation performed:** static source inspection and correction of all six controller findings; no runtime execution
5. **Slice count:** 1 (S1); S2 not started
6. **Blocking questions:** none from author side; controller must execute compile + focused + full verification
7. **Residual owners:** controller owns plan §9 verification; S2 owns lifecycle/concurrency
