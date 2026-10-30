# PA8 V008 Evidence Foundation — Spec Reconciliation (A/A)

- Task: `pa8-reconcile-mimopro-20261030-172013`
- Gate: `reconcile` (bounded documentation reconciliation only; no implementation, no review)
- Author/provider: MiMo-V2.6-Pro (plan/fix author)
- Date: 2026-10-30
- Run: `/tmp/sub-agents.pa8-reconcile.9l8M9D`
- Inputs (read-only): active PA8 controller checkpoint; `docs/gateflow/pa8-v008-evidence-foundation-plan.md`; `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-1.md` §3; `docs/reviews/plan-review-20261030-164045.md`
- Allowed writes (exactly three): `docs/14-production-data-model-and-flyway-schema-spec.md`; `docs/gateflow/pa8-v008-evidence-foundation-plan.md`; this artifact
- Historical artifacts `plan-fix-1` and the initial planreview remain read-only. Controller is read-only. Backend, CI, README, Git state are read-only. No stage/commit/push/PR/merge/delegation/build/secrets.

---

## 1. User decision

On **2026-10-30** the user explicitly **APPROVED A/A reconciliation of BD1/BD2**. This removes the prior design-authorization blocker. MiMo-V2.6-Pro remains plan/fix author. Implementation is still gated on independent re-review of the seven previously repaired findings and of this reconciliation; this gate itself performs **no** implementation and **no** review.

| Decision | Approved choice | Rejected / not chosen |
| --- | --- | --- |
| **BD1** (PR-005) — deferred FK `ON UPDATE`/`ON DELETE` action | Ordinary S3 FKs remain `ON UPDATE RESTRICT ON DELETE RESTRICT` (`confupdtype='r'`, `confdeltype='r'`). The two reciprocal V008 creation-cycle FKs **retain §11.5 EXACT DDL** (no action clause) with PostgreSQL default **NO ACTION** DEFERRABLE INITIALLY DEFERRED (`confupdtype='a'`, `confdeltype='a'`, `condeferrable=true`, `condeferred=true`). No cascade. | Alternative B (RESTRICT on the deferred FKs / §11.5 DDL amendment) is not chosen. No equivalence claim between RESTRICT and NO ACTION. |
| **BD2** (PR-001) — deferred FK vs creation-cycle enforcement | Classified enforcement, not one error for all shapes: (1) FK-satisfied cycle contradiction → `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`; (2) genuinely absent FK parent → `23503` naming the exact deferred constraint; (3) duplicate `EVIDENCE_CREATED` → immediate `23505` on `uq_evidence_created_event`. Mismatched source/owner: pin event-first fixture insertion and exact expected enforcement; isolate the target violation when multiple constraints could win. Either pair insertion order still succeeds (T-S3-08). Full transaction rollback leaves no evidence/event/artifact/Outbox on failure. Use a valid FK-satisfied decoy non-creation event to prove the cycle guard — do **not** convert a general missing-`EVIDENCE_CREATED` requirement into missing-ANY-event. Commit-aware helper required for deferred failures. | Arbitrary union-of-error outcomes (`{23503, EVIDENCE_CREATION_CYCLE_VIOLATION}`) are not chosen. Loosening named constraints or assertions is forbidden. |

---

## 2. Precise spec changes (`docs/14-production-data-model-and-flyway-schema-spec.md`)

Only directly contradictory V008 prose/acceptance rows were amended. Tables, columns, keys, grants, trigger names, waves, and §11.5 exact DDL are unchanged.

| Location | Change |
| --- | --- |
| §5.5 `:2613` | Overbroad "All S3 FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT`" replaced: ordinary S3 FKs are RESTRICT (`r`/`r`); the two V008 cycle FKs retain §11.5 exact DDL with default NO ACTION (`a`/`a`) DEFERRABLE INITIALLY DEFERRED. Added explicit catalog pin for the ten V008 FKs (eight ordinary `r`/`r`; two deferred `a`/`a`). No cascade. |
| §5.5 rows `:2623`, `:2626` | DEFERRABLE_FK creation-timing cells now pin `confupdtype='a'`, `confdeltype='a'` (§11.5 exact DDL, default NO ACTION). |
| §4.43 reciprocal creation contract | "missing, duplicate, wrong-type, wrong-evidence-owner or mismatched → `EVIDENCE_CREATION_CYCLE_VIOLATION`" replaced with the BD2 classification (FK-satisfied → `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`; absent parent → `23503` exact deferred constraint; duplicate → immediate `23505`). |
| INV-25 | Same uniform-error over-claim replaced with the BD2 classification. |
| §7.4 Evidence creation cycle row | Same over-claim replaced with the BD2 classification. |
| §9.28 step 7 | Same over-claim replaced with the BD2 classification. |
| §10.22 post-table paragraph | Added enforcement-precedence note: deferred RI (`23503`) for absent parents; cycle triggers (`P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`) for FK-satisfied contradictions; immediate `23505` on `uq_evidence_created_event` for duplicates. Trigger predicate rows themselves retained (they describe only the cycle-trigger outcome). |
| T-S3-09 | Now specifies the FK-satisfying decoy non-creation event for `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`, and a separate genuinely-absent-parent class → `23503` naming `fk_evidence_source_event`. Preserves "no matching `EVIDENCE_CREATED`" intent without converting it into missing-ANY-event. |
| T-S3-10 | Pinned immediate `23505` on `uq_evidence_created_event` (not a commit-time cycle error). |
| T-S3-11 | Pinned event-first insertion and exact expected enforcement for mismatched source/owner; both parents present for wrong-owner; isolate target violations; no SQLSTATE union. Either pair insertion order still succeeds (T-S3-08). |

**Unchanged in doc-14:** §11.5 step 7 exact ALTER statements (`:4249-4250`); all table/column/key/grant/trigger names; wave order; V009 sharing content; function-wave claims; forward-only migration policy.

---

## 3. Precise plan changes (`docs/gateflow/pa8-v008-evidence-foundation-plan.md`)

| Location | Change |
| --- | --- |
| Header status | NOT READY / blocked banner replaced with READY-for-independent-re-review-then-implementation; records user approval 2026-10-30; notes the seven previously repaired findings are **not** review-accepted. |
| §3.4 / §3.6 deferred FK rows | Catalog pin `confupdtype='a'`, `confdeltype='a'` (BD1). |
| §3.7 | PR-005 blocked text replaced with BD1 resolution + catalog pin. Classification paragraph now cites `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`, the controller probe path `/tmp/pa8-fk-probe.kKLLwK/result.log`, and BD2 resolution. §11.5 DDL block retained verbatim. |
| §3.10 FK row | Catalog pin `r`/`r` for eight ordinary, `a`/`a` for two deferred. |
| §4 order-sensitive table | Exact classified errors; pinned event-first for mismatched identity; parent-delete of deferred FK is `expectCommitFailure` (NO ACTION); removed Alt B / SQLSTATE-union language. |
| §7.3 S1 FK assertions | Assert all four catalog columns for all 10 FKs (`r`/`r` ordinary; `a`/`a` deferred + `condeferrable`/`condeferred`). |
| §9 S2 classification table | BD2-resolved executable contract: decoy construction for cycle guard; absent parents `23503`; duplicate immediate `23505`; pinned event-first mismatched identity; no union; parent-delete = `expectCommitFailure`. |
| §9 S2 method bullets + assertions | `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`; isolate target violations. |
| §10 helper table | `expectCommitFailure` covers `23503` and `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`. |
| §12 | Blocked-decision bullet replaced with resolved BD1/BD2 record + pointer to this artifact. |
| §14 / §15 | Blocking-questions and checklist no longer require PR-001/PR-005 resolution; checklist requires confirming BD1/BD2 record and independent re-review of the seven repaired findings. |

**Unchanged in plan:** two behavioral slices; progressive 51→54→57 source/CI/XML green; V001–V007 SHA-256 freeze; forward-only migration (no down/U/R); no service/production scope; full 39-check map; ACL negatives including `aclexplode` PUBLIC and foreign row-lock 42501 probes; all prior necessary corrections from plan-fix-1 (PR-002/003/004/006/007/008/009).

---

## 4. PR-001 / PR-005 mapped to resolved candidates

| Review finding | fix-1 candidate | Approved resolution | Where recorded |
| --- | --- | --- | --- |
| **PR-005** (BD-1) deferred FK action RESTRICT vs NO ACTION | Alt A (recommended): §11.5 exact DDL governs, amend §5.5 `:2613` | **A accepted.** §5.5 amended; §11.5 DDL unchanged; catalog `a`/`a` pinned for the two deferred FKs, `r`/`r` for the eight ordinary. | doc-14 §5.5; plan §3.7/§3.10/§7.3; this artifact §1–§2 |
| **PR-001** (BD-2) T-S3-09/T-S3-11 uniform SQLSTATE | Alt A (recommended): narrow to FK-satisfied class; absent parents `23503`; duplicates `23505` | **A accepted**, with decoy non-creation event (not missing-ANY-event), pinned event-first mismatched identity, isolated fixtures, commit-aware helper. | doc-14 §4.43/INV-25/§7.4/§9.28/§10.22/T-S3-09/10/11; plan §3.7/§4/§9; this artifact §1–§3 |

---

## 5. Actual validation performed

| Check | Method | Result |
| --- | --- | --- |
| Controller PA8 checkpoint, plan, plan-fix-1 §3, initial planreview | Read-only consult | Bound goal and prior blocked decisions confirmed |
| doc-14 contradictory lines | Bounded reads of §5.5, §4.41–4.43, §7.4, INV-25, §9.28, §10.22, §11.5, T-S3-08…12/30 | Only the rows listed in §2 amended; §11.5 `:4249-4250` untouched |
| BD1 catalog pin | Textual pin `confupdtype`/`confdeltype` = `a`/`a` (two deferred), `r`/`r` (eight ordinary) | Present in doc-14 §5.5 and plan §3.7/§3.10/§7.3 |
| BD2 classification | Textual match to user-approved classes (P0001 cycle / 23503 absent / 23505 duplicate) | Present in doc-14 and plan; no union-of-error outcome remains |
| Probe evidence label | Cited as **controller-executed** PostgreSQL 16.15 `/tmp/pa8-fk-probe.kKLLwK/result.log`; no equivalence claim | NO ACTION DELETE defers to COMMIT; RESTRICT DELETE/UPDATE fail immediately despite FK insertion deferral |
| Cycle trigger-order runtime proof | **Not fabricated** | Remains future S1/S2 evidence |
| Writes | This gate | Exactly three allowed files (see §7) |
| PostgreSQL / Flyway / Maven / Docker / Git | — | **Not run** (documentation-only reconcile gate) |

---

## 6. Unchanged shape and scope

- Two behavioral slices (S1 catalog/ACL/FK; S2 lifecycle/immutability/concurrency).
- Progressive source=CI=XML green: 51 → 54 (S1) → 57 (S2).
- V001–V007 SHA-256 freeze; forward-only V008 migration; no down/repeatable/U/R file.
- No application services, upload APIs/workers, COS/KMS, provider/cloud/IVR integration, production wiring, or V009 sharing.
- Full 39-check witness map, ACL negatives (`aclexplode` PUBLIC, foreign row-lock 42501), `V008_ROLES` constant rule, commit-aware `expectCommitFailure` helper, and all other plan-fix-1 corrections retained.
- Seven previously repaired findings (PR-002/003/004/006/007/008/009) remain **pending independent re-review** — not called review-accepted.

---

## 7. Files written by this gate

| File | Action |
| --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | **modified** (BD1/BD2 bounded amendments only) |
| `docs/gateflow/pa8-v008-evidence-foundation-plan.md` | **modified** (BD1/BD2 resolutions recorded; blocked-decision language removed) |
| `docs/gateflow/pa8-v008-evidence-foundation-spec-reconciliation.md` | **created** (this artifact) |

No other file was written. No Git state was changed. No delegation was performed.

---

## 8. Remaining runtime evidence (owner / destination)

| Remaining evidence | Owner | Destination |
| --- | --- | --- |
| Cycle trigger-order runtime proof (`RI_ConstraintTrigger_*` before `trg_*`) | implementer | S1/S2 container tests on PostgreSQL 16 with `expectCommitFailure` |
| Decoy-non-creation fixture yields `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` | implementer | S2 `v008CreationCycleBothOrdersAndTerminalLifecycleBackstops` |
| Both insertion orders commit (T-S3-08) | implementer | S2 |
| Immediate `23505` on `uq_evidence_created_event` | implementer | S2 |
| Catalog `confupdtype`/`confdeltype`/`condeferrable`/`condeferred` values as pinned | implementer | S1 `v008ForeignKeyAndDeferredCreationCycleFkBackstops` |
| Parent-delete of deferred-FK parent via `expectCommitFailure` | implementer | S1 |
| Full T-S3-12 transaction rollback leaves no evidence/event/artifact/Outbox | implementer | S2 |
| Independent re-review of PR-002/003/004/006/007/008/009 repairs | independent reviewer | next plan-review gate |
| Independent re-review of this reconciliation (BD1/BD2) | independent reviewer | next plan-review gate |
| Q11 WORM/storage retention, Q22 ticket TTL, legal retention | existing doc-14 owners | deferred production inputs |
| Production role provisioning / runtime switching | Platform/Database/Security | production-readiness (not PA8) |

---

## 9. Reconcile-gate completion

1. **Verdict:** `complete` (bounded documentation reconciliation).
2. **Artifacts:** the three files in §7.
3. **Exact files changed:** the three above; nothing else.
4. **Validation actually performed:** see §5. Document-only; controller-executed probe cited, not re-run.
5. **Slice count:** unchanged (2). Rationale unchanged.
6. **Blocking questions:** none for BD1/BD2. Implementation still requires independent re-review of the seven repaired findings and of this reconciliation.
7. **Residual owners:** see §8.

**STOP.** No implementation, no review, no commit/push/PR/merge. End of reconcile gate.
