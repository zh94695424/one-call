# PA8 V008 Evidence Foundation — Plan Fix 2

- Task label: `pa8-planfix3-mimo26-20261030-174746`
- Gate: `plan fix` (bounded plan execution/evidence defect fixes only; no implementation)
- Author/provider: MiMo-V2.6-Pro (plan author, returning for bounded fixes)
- Date: 2026-10-30
- Revised plan: `docs/gateflow/pa8-v008-evidence-foundation-plan.md` (SHA-256 after this fix: `7c51daa15a32179c620fc66ada7cc52ced1b4feb843d4539f95632a92409a1cd`)
- Fix artifact: `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-2.md` (this file)
- Review input: `docs/reviews/plan-review-20261030-174104.md` (fresh independent re-review; SHA-256 of pre-fix plan `c483ede26a55265650249c2d4f7d4f02584a4502282ac2e5502a40f1b5118f55`)
- Controller acceptance: RR-001..006 are concrete plan execution/evidence defects; no new product/design decision required. User-approved A/A remains binding and resolved.
- Scope fence: only the current plan plus this NEW fix-2 artifact may be written. Spec/source/tests/CI/README/controller/historical reports (including fix-1 and plan-review-20261030-164045) remain read-only. No commit/stage/push/PR/merge/build/delegation/next gate.

---

## 1. Disposition summary

| RR | Severity (review) | Controller class | Concrete choice applied | State after this fix |
| --- | --- | --- | --- | --- |
| RR-001 | 中 | accepted plan execution/evidence defect | Pinned parent/child fixtures; savepoint-scoped `DISABLE TRIGGER trg_media_object_immutable` for protected media parents; deferred pair construction on `evidence_record` with event-only child | **fixed** (§3.1) |
| RR-002 | 中 | accepted plan execution/evidence defect | `ck_evidence_source_ref` = enforced-by-NOTNULL exception; `23502` + catalog/positive witness; no `23514` claim; no `NOT NULL` drop | **fixed** (§3.2) |
| RR-003 | 中 | accepted plan execution/evidence defect | V007 isolated-preempting savepoint for enum checks; distinct `event_id`/PK for T-S3-10; two-record/two-event crosslink for wrong-owner | **fixed** (§3.3) |
| RR-004 | 中 | accepted plan execution/evidence defect | `expectCommitFailure` introduced in S1 where first used; S2 reuses; checklist/helper/allowed-files aligned | **fixed** (§3.4) |
| RR-005 | 中 | accepted plan execution/evidence defect | Owner `P0001` vs runtime `42501` explicit; valid `call_record` prerequisites; distinct sentinel failure; three named immutable triggers in cleanup | **fixed** (§3.5) |
| RR-006 | 中 | accepted plan execution/evidence defect | Failsafe `TEST-*PostgresContainerIT.xml` at 54/57 everywhere; Surefire stays 10 | **fixed** (§3.6) |

**Plan verdict after this fix: READY for independent re-review then implementation.** No re-review acceptance is claimed. A/A (BD1/BD2) remains binding and is not reopened. No product/schema/wave/service scope was added.

---

## 2. Actual checks consulted (not reviewer text alone)

| Assertion | Actual source | Result |
| --- | --- | --- |
| `expectSqlFailure` is statement-time only | `PostgresContainerIT.java:3626-3651` | savepoint + `executeUpdate`; cannot observe commit-time `23503`/`P0001` |
| V007 representative parent-delete on mutable parent | `PostgresContainerIT.java:8253-8277` | `customer_account` (no delete trigger) + named child `consent_record`; `23503` `fk_consent_account` |
| V007 isolated-preempting helper | `PostgresContainerIT.java:8889-8924` | savepoint `DROP CONSTRAINT` preemptors, probe, rollback, `pg_constraint` restoration asserted |
| V007 owner `P0001` vs runtime `42501` | `PostgresContainerIT.java:9130-9150` | `switchLocalRole("activation_consent_owner")` for `P0001`/`CONSENT_TRANSITION_APPEND_ONLY`; `activation_consent_role` for `42501` |
| V007 named-trigger cleanup | `PostgresContainerIT.java:10068-10123` | `DISABLE TRIGGER` exact names, dependency-order delete, `ENABLE`, `tgenabled='O'` asserted |
| Failsafe binds container ITs | `backend/pom.xml:136-156` | `container-smoke` profile → `maven-failsafe-plugin` includes `**/PostgresContainerIT.java` |
| CI parses Failsafe XML | `.github/workflows/ci.yml:73` | `target/failsafe-reports/TEST-*PostgresContainerIT.xml`; Surefire loop at `:139-141` is the separate 10-test check |
| README distinguishes Surefire vs Failsafe | `backend/README.md:37,45,71-72,106` | default `verify` ≠ container smoke; container evidence is Failsafe 51→57 |
| `source_ref_id` NOT NULL + redundant CHECK | plan §3.4 / doc-14 catalog (`uuid NO`, `source_ref_id IS NOT NULL`) | `23502` preempts `23514`; confirmed in re-review RR-002 |
| Append-only parents block literal FK delete | plan §3.2/§3.6 `trg_media_object_immutable`, `trg_evidence_event_immutable` | literal DELETE raises `P0001`, not `23503` |
| `evidence_record` guard is UPDATE-only | plan §3.4 `trg_evidence_record_guard BEFORE UPDATE` | parent DELETE is not trigger-blocked; deferred FK can be reached |

No Docker, Maven, Flyway or PostgreSQL session was run by the plan author in this fix. Controller probe `/tmp/pa8-fk-probe.kKLLwK/result.log` timing facts are unchanged and not re-claimed as author-executed.

---

## 3. Accepted-defect fixes (RR-001..006)

All plan text changes are in `docs/gateflow/pa8-v008-evidence-foundation-plan.md`. Each subsection states one concrete choice — no alternative/pick-one remains.

### 3.1 RR-001 — FK parent/child fixtures and deferred parent-delete construction

**Defect:** S1 parent-delete matrix targeted append-only parents (`media_object`, `evidence_event`) where `trg_*_immutable` raises `P0001` before the FK; deferred parent-delete lacked an event-only child, snapshot/restore proof, and INSERT-trigger preemption exclusion; media-object FK probes did not isolate the child.

**Concrete choice:**
- Pinned one parent/child pair per ordinary RESTRICT FK in S1 `v008ForeignKeyAndDeferredCreationCycleFkBackstops` (table in plan §9 S1).
- Unprotected parents (`business_subject`, `call_record`, `inbound_session`, `usage_reservation`, `evidence_record`) delete directly → immediate `23503`.
- Protected `media_object` parent: savepoint-scoped `DISABLE TRIGGER trg_media_object_immutable` **solely** to reach the named FK (as migration identity / `evidence_owner`), then `23503`; rollback; assert `pg_trigger.tgenabled='O'` + constraint catalog. No constraint is weakened or dropped.
- `fk_recording_media_object` child = recording only (no artifact). `fk_evidence_artifact_media` child = artifact only (no recording). Named isolation so unrelated FKs cannot win.
- Deferred parent-delete (only `fk_evidence_event_record` direction): committed valid `evidence_record` + matching `EVIDENCE_CREATED` pair, **no artifact children**; snapshot starting counts; new transaction `DELETE evidence_record` (guard is UPDATE-only); `expectCommitFailure(..., "23503", "fk_evidence_event_record")`; after failed commit the pair **survives** (rollback restores pair — do not claim the pair vanishes). Delete-only transaction ⇒ no pending INSERT creation-cycle trigger can preempt the target. Do not delete `evidence_event` as the parent.

### 3.2 RR-002 — `ck_evidence_source_ref` enforced-by-NOTNULL exception

**Defect:** map promised `23514` on NULL `source_ref_id`; column `NOT NULL` fires `23502` first; the CHECK is redundant.

**Concrete choice:**
- Keep the exact CHECK catalog entry and the 39 named checks inventory unchanged.
- Witness: production-schema `expectSqlFailure(..., "23502", ...)` on NULL `source_ref_id` **plus** positive witness (exact CHECK definition present in `pg_constraint`; valid non-NULL row commits).
- Do **not** drop `NOT NULL`; do **not** claim `23514`/`ck_evidence_source_ref` fired.
- 39-check coverage language now states 38 × `23514` (or isolated-preempting `23514`) + 1 × enforced-by-NOTNULL exception.

### 3.3 RR-003 — enum preemption, duplicate-event fixture, wrong-owner construction

**Defect:** `ck_recording_status`/`ck_evidence_status` overlap state-shape checks (non-deterministic exact name); T-S3-10 could hit `uq_evidence_event_domain_id` instead of `uq_evidence_created_event`; wrong-owner/source fixtures were a vague union; reviewer open question left two constructions unresolved.

**Concrete choice:**
- Enum checks: reuse the existing V007 `expectV007CheckWithIsolatedPreempting` savepoint pattern (`PostgresContainerIT.java:8889-8924`). Named preemptors: `ck_recording_state_shape` preempts `ck_recording_status`; `ck_evidence_state_shape` preempts `ck_evidence_status`. Savepoint-drop preemptor, probe target `23514`, rollback, full catalog restoration.
- T-S3-10: second `EVIDENCE_CREATED` uses **distinct `event_id` and distinct PKs** so only `uq_evidence_created_event` can fail `23505`.
- Wrong-owner T-S3-11 (reviewer open question 1): **two-record/two-event crosslink** — Record A↔Event B, Record B↔Event A, distinct PKs/`event_id`s, both deferred FKs satisfied, committed valid parent prerequisites, events-then-records pinned insertion order. Class-level `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` only; no per-trigger isolation claim. Not a nonexistent parent; not a shared unique source.
- Source-mismatch T-S3-11: pinned event-first insertion with committed valid witness prerequisites; expect `P0001` only (no SQLSTATE union).
- T-S3-09 remains the **noncreation-decoy** construction (distinct PK, non-`EVIDENCE_CREATED` type, shared `(domain, event_id)` identity) — single class, single expected error.

### 3.4 RR-004 — `expectCommitFailure` allocated to S1

**Defect:** S1 required `expectCommitFailure` for deferred parent-delete; plan assigned the helper to S2.

**Concrete choice:**
- Introduce `expectCommitFailure(connection, sqlstate, messageFragment, setup)` in **S1** `v008ForeignKeyAndDeferredCreationCycleFkBackstops` (first use).
- S2 **reuses**; checklist, helper table (§10), allowed-files row (§6), and S2 "Implementation decisions" all say introduce-in-S1 / reuse-in-S2.
- No future-slice prerequisite. S1 CI expected-set stays 54; S2 stays 57.

### 3.5 RR-005 — roles, call parent, sentinel failure, cleanup

**Defect:** guard/append-only/rollback fixtures did not pin executing role or valid `call_record`; cleanup did not name the three immutable triggers.

**Concrete choice:**
- Owner/`evidence_owner` (or migration identity) runs every `P0001` proof. Never run an immutable-trigger proof as `evidence_role` (that is `42501` and stays in S1 ACL only).
- All recording inserts require a committed valid `ivr.call_record` parent (and valid `media_object` when referenced) before the insert.
- Recording-only and T-S3-12 rollbacks: successful valid writes first, **observe inserted counts > 0**, then a **distinct sentinel failure** after those writes (not a missing-parent error standing in for the write set), then zero survivors.
- Method-local cleanup names exactly `trg_media_object_immutable`, `trg_evidence_artifact_immutable`, `trg_evidence_event_immutable`; same-transaction `DISABLE`/`ENABLE`; FK-safe delete both cycle sides immediate-child-first (artifact → event → record → media/parent); assert `tgenabled='O'`; after delete no queued INSERT/UPDATE cycle trigger references removed rows; never delete shared fixtures.

### 3.6 RR-006 — Failsafe container report, not Surefire

**Defect:** plan said "surefire XML must be equal at 57"; container ITs are Failsafe.

**Concrete choice:**
- Every validation equality is source = CI expected-set = **Failsafe** `target/failsafe-reports/TEST-*PostgresContainerIT.xml` `testcase` names at 54 (S1) / 57 (S2), `skipped=0/failures=0/errors=0`.
- Default Surefire `target/surefire-reports/TEST-*.xml` is the separate unchanged **10** unit/module check; never assert 54/57 there.
- Corrected §7.1, S1 assertions, S2 tests/commands, slice-count rationale, checklist.

---

## 4. Original PR-001 / PR-009 closure candidates

No original finding is claimed review-accepted. The following are **closure candidates for the next independent re-review only**.

| Original finding | Prior re-review state | Closure candidate after this fix | Actual check |
| --- | --- | --- | --- |
| PR-001 — deferred FK vs creation-cycle SQLSTATE contract | 部分修复 (contract fixed; S1 executable evidence incomplete via RR-001/RR-004) | **candidate: closed** — classified BD2 contract unchanged; S1 now owns `expectCommitFailure`, pinned deferred parent-delete pair, and named parent/child FK fixtures | plan §3.7 classification + §9 S1 FK method + §9 S2 classification table |
| PR-009 — incomplete S2 fixtures and 39-check map | 部分修复 (map exists; RR-002/003/005 witness/fixture defects) | **candidate: closed** — NOTNULL exception explicit; enum-preemptor names pinned; T-S3-10 distinct ids; wrong-owner crosslink; owner/call-parent/sentinel/cleanup pinned | plan §9 39-check map + S2 method bodies + §10 helper rows |

PR-002/003/004/005/006/007/008 remain at their prior fixed/resolved states and are untouched by this fix.

---

## 5. Reviewer open questions — dispositions

| Open question | Disposition (single choice) |
| --- | --- |
| Q1 wrong-owner T-S3-11 isolation | **Resolved in plan text:** two-record/two-event crosslink construction; class-level `P0001` contract only; no per-trigger isolation claim. |
| Q2 §8.1 V003 evidence-absence wording | **Resolved in plan text:** full-connection V003 asserts `nspname IN ('sharing')` only; historical evidence-absent intent lives in `stagedV007EvidenceSchemaAbsent`. No "or move" alternative remains. |
| Q3 doc-14 §11.5 step placement | **Unchanged:** design-owner optional clarification; not a PA8 blocker. |
| Q4 V008 append-only error text | **Unchanged:** implementer pins stable table-agnostic fragment at implementation if exact-message evidence is desired; tests assert `P0001` + stable fragment. |

---

## 6. Final validation / blockers

- **Artifacts written (exactly two):**
  1. `docs/gateflow/pa8-v008-evidence-foundation-plan.md` (current plan, bounded edits)
  2. `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-2.md` (this file)
- **Path/actual validation:** source evidence in §2 consulted via targeted reads; plan edits confined to RR-001..006 sites plus status/helper/checklist alignment; spec/source/tests/CI/README/controller/historical reports unmodified; no migration/implementation/commit/stage/push/PR/merge/build/delegation/next gate.
- **A/A:** user-approved BD1/BD2 remains binding; no schema/service/wave expansion.
- **Unresolved alternatives/pick-one:** none in the plan after this fix (§4 V003 row, S1/S2 helper allocation, FK fixtures, check witnesses, report paths are each single-choice).
- **Blockers to implementation:** none from this artifact. Required next gate is a **fresh independent re-review** of the revised plan (SHA-256 `7c51daa15a32179c620fc66ada7cc52ced1b4feb843d4539f95632a92409a1cd`) before implementation. This fix does **not** claim that re-review accepted anything.
- **Completion status:** plan-fix-2 complete; stopped after writing the two artifacts.
