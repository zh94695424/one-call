# PA7 V007 S1 Code-Review Fix 1

| Field | Value |
| --- | --- |
| Task label | `pa7-resume-fix-mimo26flash-20261030-114757` |
| Gate | S1 code-review fix (accepted findings only) |
| Work unit | `PA7-v007-activation-consent-foundation` |
| Base | `64b576abd4efce956d51d4e57a6b8afeeb13cd45` |
| Pre-fix test SHA | `797170378328236157E32210F2C1BD97EE98F2089B35BDF241639D05B16378AA` (21,812 lines) |
| Post-fix test SHA | `b568fc4261b8ac37d359835a3533b418e51ad6f4e216da2e06a1eecf92ca0412` (21,886 lines) |
| V007 migration SHA | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` (unchanged) |
| Reviews | `docs/reviews/code-review-20261030-113444-A.md`, `-B.md` (not edited) |

Sole modified path: `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`. Exactly 48 `@Test` methods; no S2 scope; no migration/SQL/CI/README/report changes; no grant changes.

## Accepted fixes

1. **A/F1 + B/F-01 (medium) — executable row-lock denials** in `assertV007RuntimeDenialsAndLegalUpdateControl`.
   - `activation_consent_role`: savepoint-isolated `expectSqlQueryFailure` `42501` for `FOR UPDATE` **and** `FOR SHARE` on `subject_number.business_subject(id)`, `customer_iam.customer_account(id)`, `organization_access.organization(id)`, `organization_access.organization_membership(organization_id)` (permitted columns only; random local IDs, zero rows).
   - `ivr_role`: `service_activation(id)` `FOR UPDATE`/`FOR SHARE` on the existing activation row; `consent_record(subject_id)` (ivr lacks `id`) on the existing subject row.
   - `catalog_order_role`: `service_activation(id)` on the existing activation row; `consent_record(id)` with random ID.
   - Legal head `UPDATE`, prior runtime `42501`/owner `P0001` controls, and `CREATE` denial preserved; reuse of existing `expectSqlQueryFailure` (savepoint rollback) matches V005/V006 precedent.
2. **B/F-02 (low) — cleanup verify covers consents.** `cleanupV004ConcurrentFixtures` verify block: added per-ID `count(*) = 0` assertion for every collected `consent_record` ID after the `payment_event` loop. Cleanup order, named trigger disable/restore, `tgenabled = 'O'` check, and all prior checks unchanged.
3. **A/F3 + B/F-03 (low) — RESTRICT name pin decoupled.** Representative parent-delete fixture: removed the `v007InsertIvrTransition` call so `customer_account` is referenced only by `fk_consent_account`; `expectSqlFailure(connection, "23503", "fk_consent_account", ...)` retained verbatim. All ten missing-parent FK probes and the positive control unchanged; comment updated.
4. **A/F2 (low) — exact trigger event set.** `assertV007FunctionsAndTriggersExact`: query now selects `t.tgtype`; per-trigger expected array carries the full bitmask; exact equality `tgtype = 27` (trg_consent_record_immutable, trg_activation_transition_immutable) and `tgtype = 19` (trg_service_activation_guard), rejecting added INSERT(4)/TRUNCATE(32)/INSTEAD(64) bits. Relation/function/existence/uniqueness assertions preserved.

Deferred per controller: A/F4 (S2 immutability probes), A/F5 (historical artifact hash), runtime build-timestamp note.

## Validation performed (actual)

- `sha256sum` on pre-fix test + migration (both matched frozen values before edits; migration re-verified identical after).
- Four `Edit`-tool patch applications, re-read of each edited region post-edit.
- `git diff --check` on the test file: clean.
- `grep -c "^[[:space:]]*@Test"` → 48 (raw `@Test` string count 49 = 48 methods + the known pre-existing inline comment).
- `wc -l` / post-fix `sha256sum` recorded above.
- `git status --short`: no new modified paths beyond the four already modified at task start.

Not run (per task boundary, not attempted): Maven/Java/Docker; 48-test runtime verification is **pending controller**.
