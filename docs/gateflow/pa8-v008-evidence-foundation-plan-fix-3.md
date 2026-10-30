# PA8 V008 Evidence Foundation — Plan Fix 3

- Task label: `pa8-planfix4-mimo26-20261030-180824`
- Run directory: `/tmp/sub-agents.pa8-planfix4.tuuEOd`
- Gate: `plan fix` (bounded RR-007/RR-008 only; no implementation)
- Author/provider: MiMo-V2.6-Pro
- Date: 2026-10-30
- Revised plan: `docs/gateflow/pa8-v008-evidence-foundation-plan.md` (SHA-256 after this fix: `0461ac69f24d7994e7a357ce9a92d749ab0d1bf47414992b37f7edf23b9a7c04`)
- Fix artifact: `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-3.md` (this file)
- Review input: `docs/reviews/plan-review-20261030-180559.md` (controller accepts RR007/RR008; full findings + closure table read)
- Scope fence: only the current plan plus this NEW fix-3 artifact may be written. Spec/controller/history/source/CI/prior fix-2 remain read-only. Goal/scope and user-approved A/A unchanged. No new schema/product/service/design decision. No build/runtime/commit/stage/push/PR/merge/delegation/next gate.

---

## 1. Disposition summary

| RR | Severity | Concrete choice applied | State after this fix |
| --- | --- | --- | --- |
| RR-007 | 中 | Source-mismatch = FK-satisfied three-row decoy (R/D/C); class-level `P0001` only; no named trigger; no queue-order reliance; no SQLSTATE union | **fixed** (§3 below) |
| RR-008 | 中 | Local cleanup order includes `recording`; shared method-local helper; fresh-connection per-id zero residue; S1/S2 committed pairs cleaned after observation | **fixed** (§3 below) |

**Plan verdict after this fix: READY for a fresh independent re-review then implementation.** No re-review acceptance is claimed. A/A (BD1/BD2) remains binding and is not reopened. Wrong-owner two-record/two-event crosslink is unchanged.

---

## 2. Actual read-only checks consulted

| Assertion | Actual source | Result |
| --- | --- | --- |
| RR-007/RR-008 findings + closure table | `docs/reviews/plan-review-20261030-180559.md` (full) | Both accepted as concrete plan execution/evidence defects |
| FK-satisfied isolation rule | same review quotes doc-14 T-S3-11 | Fixture must satisfy both deferred FKs; no SQLSTATE union |
| `fk_evidence_source_event` / `fk_evidence_event_record` | plan §3.4/§3.6; review §RR-007 | Two-row event-first leaves record-side source FK unsatisfied |
| V007 per-id zero residue | review cites `PostgresContainerIT.java:10068-10167` | `cleanupV007S2LocalRows` precedent; shared helper shape |
| `recording` is child of media + call | plan §3.3 `fk_recording_call`/`fk_recording_media_object` | Omitting `recording` blocks media/call cleanup |
| Prior fix-2 cleanup wording | `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-2.md:103` (history only) | Old order omitted `recording`; plan text corrected here, fix-2 not edited |
| V001–V007 digests | `sha256sum` on the seven migration files | All match plan §6.1 |
| Spec bytes | `docs/14-production-data-model-and-flyway-schema-spec.md` | SHA-256 `8a4574272a9e411185e00fe088e530bf561069c703c1ce9eee7313fa0bfed685` unchanged |
| Fix-2 / review / controller | `sha256sum` | fix-2 `7f15d48…` unchanged; review `4882d8d…` unchanged; controller remains the pre-existing dirty unrelated change |
| Whitespace | `git diff --check` | No output |

No Docker, Maven, Flyway or PostgreSQL session was run. No source/CI/README/test file was read for mutation.

---

## 3. Accepted-defect fixes (RR-007, RR-008)

All plan text changes are in `docs/gateflow/pa8-v008-evidence-foundation-plan.md`. Each subsection states one concrete choice.

### 3.1 RR-007 — source-mismatch fixture is FK-satisfied; class-level only

**Defect:** T-S3-11 source-mismatch used a two-row event-first construction. Record `R.source=(EVIDENCE,X)` had no matching `evidence_event(event_domain,event_id)=(EVIDENCE,X)` (creation event used `Y≠X`), so `fk_evidence_source_event` was unsatisfied. Expected `P0001` relied on deferred-queue order and named `trg_evidence_event_creation_cycle`.

**Concrete choice (controller-specified):**
- Three rows, distinct PKs, X≠Y:
  - Record `R.source=(EVIDENCE,X)`
  - Noncreation decoy `D.event=(EVIDENCE,X)`, `D.evidence_id=R`, `event_type ≠ EVIDENCE_CREATED`
  - Creation event `C.event=(EVIDENCE,Y)`, `C.evidence_id=R`, `event_type=EVIDENCE_CREATED`
- Both deferred FKs satisfied (source FK via D; `fk_evidence_event_record` via both).
- Committed valid parent prerequisites first; insert events D,C then record R.
- COMMIT fails class-level `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` only. No named-trigger specificity; no queue-order reliance; no SQLSTATE union.
- Removed the contradictory event-first-only two-row claim everywhere (§3.7, §4 table, S2 classification table, S2 T-S3-11 bullet).
- Wrong-owner two-record/two-event crosslink unchanged.

### 3.2 RR-008 — cleanup includes `recording`; shared helper; per-id zero residue

**Defect:** S2 concurrency cleanup omitted `evidence.recording` and the V007-style per-id zero-residue assertion; committed winner recording could survive or block media/call cleanup. S1 committed deferred-parent pair and S2 T-S3-08 committed pairs lacked explicit cleanup.

**Concrete choice:**
- Local FK-safe delete order: **artifacts → events → records → recording → media → method-local IVR/reservation/subject parents** (each parent domain in its existing own FK-safe order).
- Three named immutable triggers `DISABLE`/`ENABLE` in the same cleanup transaction; deferred cycle pair deleted together; no pending creation-trigger INSERT/UPDATE in the cleanup tx.
- Fresh connection **per-id** zero rows after cleanup commits for all recorded fixture IDs/tables (not shared/global counts).
- Owner/migration identity deletes with correct schema ownership (`evidence_owner`/migration identity for evidence tables; `ivr_owner`/`subject_number_owner`/`entitlement_metering_owner` for parents).
- Shared local-id cleanup helper (V007 `cleanupV007S2LocalRows` shape) used by S1 and S2 — not a separate invented framework, no new test method, no schema change.
- S1 deferred parent-delete: observe pair survival after rollback **first**, then finally clean it (observation ≠ residue).
- S2 T-S3-08 committed valid-order pairs/artifacts cleaned with the same helper after assertions.

---

## 4. Touched plan locations

| Site | Change |
| --- | --- |
| Header status | RR-007/008 + fix-3 recorded; RR-001..008 still need independent re-review |
| §3.7 classification | Source-mismatch decoy listed as FK-satisfied class; class-level-only language |
| §4 order-sensitive table (source-mismatch row) | R/D/C construction replaces event-first two-row claim |
| §4 deferred parent-delete row | Observe-then-clean discipline |
| S1 deferred parent-delete bullet | Observe-then-clean discipline |
| S2 classification table (Mismatched identity row) | R/D/C construction; class-level only |
| S2 T-S3-08 bullet | Shared-helper cleanup of committed pairs |
| S2 T-S3-11 bullet | Decoy construction pointer; wrong-owner unchanged |
| S2 concurrency cleanup bullet | Full local order incl. `recording`; per-id zero residue; ownership |
| §10 helper table | Cleanup order + shared method-local cleanup helper row |
| §15 checklist | RR-001..008 |

---

## 5. Preserved specs / history

- Goal, scope fence, frozen §3 inventories, A/A (BD1/BD2), 51→54→57, Failsafe naming, NOTNULL exception, enum/duplicate/wrong-owner, role pins, sentinel rollback: unchanged.
- `docs/14-*`, `docs/12-*`, controller, source, CI, README, V001–V007, fix-1, fix-2, prior reviews: read-only; hashes unchanged where recorded.
- Wrong-owner crosslink: unchanged.

---

## 6. Residual owners / destinations

| Residual | Owner | Destination |
| --- | --- | --- |
| Deferred trigger firing order / commit-time SQLSTATEs remain static claims | implementer | PostgreSQL 16 container tests; class-level assert; stop-and-report on mismatch |
| Sorted `containsExactly` insertion in catalog lists | implementer | S1 `assertFunctionsAndTrigger` |
| Append-only error text fragment | implementer | S2/ACL test naming |
| Doc-14 §11.5 step wording | design owner | Future doc clarification |
| Q11 WORM / Q22 TTL / legal retention / production role provisioning / V009 | existing owners | Outside PA8 |

---

## 7. Final validation / blockers

- **Artifacts written (exactly two):** (1) revised plan, (2) this fix-3 file.
- **Validation performed:** full review RR-007/RR-008 + closure table read; targeted plan reads/edits only at listed sites; `git diff --check` clean; spec SHA-256 `8a45742…` unchanged; V001–V007 SHA-256 match §6.1; fix-2/review/controller not written.
- **A/A:** unchanged; no schema/service/wave expansion.
- **Blockers:** none from this artifact. Required next gate is a **fresh independent re-review** of the revised plan (SHA-256 `0461ac69f24d7994e7a357ce9a92d749ab0d1bf47414992b37f7edf23b9a7c04`) before implementation. This fix does **not** claim that re-review accepted anything.
- **Completion status:** plan-fix-3 complete; stopped after writing the two artifacts.
