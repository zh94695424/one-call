# PA8 V008 Evidence Foundation — Plan Fix 1

- Task label: `pa8-planfix-mimopro-20261030-164854`
- Gate: `plan fix` (bounded plan-defect fixes only; no implementation)
- Author/provider: MiMo-V2.6-Pro (plan author, returning for bounded fixes)
- Date: 2026-10-30
- Revised plan: `docs/gateflow/pa8-v008-evidence-foundation-plan.md`
- Fix artifact: `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-1.md` (this file)
- Review input: `docs/reviews/plan-review-20261030-164045.md` (frozen SHA-256 `0423a6acea7c130a5df63b6903a592fc275147a6a7d415bb236b6335d8416644` of the pre-fix plan)
- Binding scope: `.pa8-plan-prompt.md` + controller active PA8 checkpoint
- Scope fence: only the two artifacts named above may be written. No source/design/controller edits, no implementation, no stage/commit/push/PR/merge/delegation, no next gate.

---

## 1. Disposition summary

| PR | Severity (review) | Disposition | State after this fix |
| --- | --- | --- | --- |
| PR-001 | 高 | **needs-more-evidence / frozen-design conflict** — not silently fixed | **blocked** (see §3) |
| PR-002 | 高 | accepted concrete plan defect | **fixed** (see §2.2) |
| PR-003 | 中 | accepted concrete plan defect | **fixed** (see §2.3) |
| PR-004 | 中 | accepted concrete plan defect | **fixed** (see §2.4) |
| PR-005 | 中 | **frozen-design conflict** — not silently fixed | **blocked** (see §3) |
| PR-006 | 中 | accepted concrete plan defect | **fixed** (see §2.6) |
| PR-007 | 中 | accepted concrete plan defect | **fixed** (see §2.7) |
| PR-008 | 中 | accepted concrete plan defect | **fixed** (see §2.8) |
| PR-009 | 中 | accepted concrete plan defect | **fixed** (see §2.9) |

**Plan verdict after this fix: NOT READY.** The seven accepted defects are repaired. PR-001 and PR-005 remain open design contradictions recorded in §3. Implementation must not start until the controller/user resolves them.

---

## 2. Accepted-defect fixes (PR-002/003/004/006/007/008/009)

All fixes are in `docs/gateflow/pa8-v008-evidence-foundation-plan.md` only. Each subsection states the defect, the actual source evidence used, and the exact plan change.

### 2.1 Evidence base actually consulted (not reviewer text alone)

| Assertion | Actual source | Result |
| --- | --- | --- |
| Migration directory is forward-only V001–V007 | `backend/src/main/resources/db/migration/` glob | exactly `V001`…`V007`; no `V008`/`U*`/`R*`/down file |
| V007 ordinary-FK pattern | `V007__activation_and_consent.sql:256-312` | all 11 FKs added by `ALTER TABLE ... ADD CONSTRAINT` **after** `RESET ROLE` (migration identity); every ordinary FK carries `ON UPDATE RESTRICT ON DELETE RESTRICT` |
| SECURITY DEFINER inventory | `V001...sql` (4× `SECURITY DEFINER` at :114/:182/:296/:453) + `V006...sql:168` | five named service functions: `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append`, `audit.append_fact`, `entitlement_metering.issue_first_trial`. `issue_first_trial` is **not** the only one. |
| Global function/trigger catalogs | `PostgresContainerIT.java:6293-6333` (35 functions), `:6423-6463` (33 triggers) | both `containsExactly` lists are all-schema and will fail when V008 adds 5 functions + 7 triggers |
| Flyway history block | `PostgresContainerIT.java:1184-1252` | asserts rows 001–007 then `resultSet.next()` is false; V008 adds an eighth row |
| V003 schema-absence probe | `PostgresContainerIT.java:17536-17539` | `count(*) ... nspname IN ('evidence','sharing')` is zero — guaranteed red once `evidence` exists |
| Shared role constants | `PostgresContainerIT.java:67-82` (`S1_ROLES`), `:149+` (`V004_ROLES`); reused at :8401, :8556, :8681, :10556, :16077, … | adding evidence roles to these constants would break every prior ACL/schema matrix |
| `expectSqlFailure` | `PostgresContainerIT.java:3626-3651` | savepoint + `executeUpdate` only; cannot observe commit-time failures |
| PUBLIC ACL expansion | `PostgresContainerIT.java:8647-8675` (and :1947-1964) | PA7 uses `aclexplode(coalesce(n.nspacl, acldefault(...)))` with `a.grantee = 0`; the file itself comments "PUBLIC is the ACL pseudo-grantee OID 0, not a pg_roles login name" |
| Foreign row-lock probes | `PostgresContainerIT.java:2664-2741` | existing `FOR UPDATE` 42501 pattern for cross-module reads |
| CI expected-set | `.github/workflows/ci.yml:77,130-138` | Python exact-name-set + `tests == len(expected)` + `skipped=0/failures=0/errors=0`; label `PA7 V007 S2 test count (51)` |
| Forward-only migration policy | doc-14 `:4203` ("S1 expects no destructive rollback. Future changes use expand/contract."), `:4773` ("V001-V009 forward-only, no backfill") | confirms PR-002 |
| §5.5 vs §11.5 deferred-FK actions | doc-14 `:2613` vs `:4249-4250` | confirms PR-005 (see §3) |
| T-S3-09/T-S3-11 wording | doc-14 `:4734`, `:4736` | confirms PR-001 (see §3) |
| RESTRICT vs NO ACTION parent-action timing | **controller-executed** PostgreSQL 16.15 probe (`/tmp/pa8-fk-probe.z5lJbH/result.log`; transient container; not author-executed) | NO ACTION parent-delete defers to COMMIT (`23503`); RESTRICT parent-delete is immediate (`23503` then `25P02`); INSERT deferral unchanged; UPDATE RESTRICT nondeferrable |

No Docker, Maven, Flyway or PostgreSQL session was run by the plan author. A **controller-executed** PostgreSQL 16.15 probe (dedicated transient container, `/tmp/pa8-fk-probe.z5lJbH/result.log`) supplies the RESTRICT vs NO ACTION parent-action timing facts used in §3.1.2. No author-executed PostgreSQL runtime verification is claimed.

### 2.2 PR-002 — rollback-migration requirement removed

**Defect:** plan required a down/repeatable/U/R/target-reverse rollback migration; repository and frozen design are forward-only. A literal `R__` file would run after every `target()` migrate and destroy V008 state.

**Plan change:**
- §1 / §2.1: "migration + rollback + …" → "migration + transaction-rollback + …"; scope row now says "transaction rollback evidence only; no down/repeatable/U/R/target-reverse migration".
- §4: replaced the reverse-drop paragraph with an explicit **"No rollback migration"** rule: transactional forced-failure tests only (T-S3-12 + recording-only rollback); test-local cleanup allowed; `R__`/`U008`/`R008`/`target()`-reverse forbidden.
- §6: deleted the entire `R__rollback_v008_evidence_media.sql` / `U008`/`R008` / `target()`-reverse allowed-files row. `V008__evidence_media.sql` is the only new migration file.
- §13: replaced the "Rollback file convention unclear" risk row with a **forbidden-artifact** row.
- §14 / §15: completion and checklist say "transaction rollback", not "rollback migration"; checklist forbids down/U/R files.

### 2.3 PR-003 — final-state catalog/history/schema-absence/role-constant updates completed

**Defect:** §8.1 listed only two must-change locations; `assertFunctionsAndTrigger`, the V003 schema-absence probe, the Flyway history block and the shared role constants are also guaranteed red.

**Plan change (§8.1 now has six rows plus a function-wave note):**
1. `assertSchemasAndRoles`: 11 schemas, `V008_ROLES` appended, `sharing` still absent. **New `V008_ROLES` constant; do not touch `S1_ROLES`/`V004_ROLES`.**
2. `assertFunctionsAndTrigger`: append the 5 `evidence.*` functions (total 40) and the 7 `evidence.*` triggers (total 40) to the all-schema `containsExactly` lists; add a parallel evidence signature/owner/SECURITY-INVOKER block.
3. `assertRiskOpenClearReopenRevisionProtocol` (~17536-17539): change `nspname IN ('evidence','sharing')` to `nspname IN ('sharing')` (or move the historical evidence-absent check to the staged-V007 capture).
4. Migration-method history block (~1184-1252): add the V008 row (`version 008`, description `evidence media`, type `SQL`, script `V008__evidence_media.sql`) and the staged-V007 `MigrateResult`/`migrationsExecuted=1` assertion.
5. `assertNoV008PlusObjects` split semantics (unchanged in intent, now cross-referenced).
6. V006/V007 callers keep staged-V007 booleans **and** the new final-state helper.

### 2.4 PR-004 — ordinary FKs added under migration identity, not inline

**Defect:** §4 steps 3–6 read as inline FK clauses under `SET LOCAL ROLE evidence_owner`; that fails `42501` on cross-schema REFERENCES.

**Plan change (§4):** explicit pattern sentence — create all five tables with checks/keys/indexes under `evidence_owner` **without any cross-schema FK clause**; `RESET ROLE`; add the 8 ordinary FKs and the 2 deferred cycle FKs by `ALTER TABLE ... ADD CONSTRAINT` under the **migration identity**, matching `V007:256-312`. Steps 3–6 now say "No FK clauses here". No REFERENCES grant is issued to runtime or owner roles.

### 2.5 PR-005 — not fixed here; see §3 (blocked)

### 2.6 PR-006 — function-wave mitigation corrected

**Defect:** risk row asserted `pg_proc` count = 5 and `issue_first_trial` is the only SECURITY DEFINER. Both are false (40 user functions post-V008; five SECURITY DEFINER service functions).

**Plan change:** §8.1 function-wave note + §13 risk row now require: (a) the five named service functions by signature with `prosecdef=true`; (b) SECURITY DEFINER user-function count exactly 5; (c) V008 adds only the five SECURITY INVOKER trigger functions and zero service-wave rows. Explicitly: do **not** assert total `pg_proc` = 5. §3.10 / §7.2 wording clarified to "the five named service functions (not total `pg_proc`)".

### 2.7 PR-007 — CI sequencing alternative deleted

**Defect:** "Alternatively, land both slices' expected-set only at slice 2" cannot keep S1 green.

**Plan change (§9 slice-1 assertion):** mandated sequencing only — S1 updates source + CI expected-set (54 names incl. rename) + README together; S2 updates to 57 + PA8 V008 label. Each accepted slice passes exact source=CI=XML equality and zero skipped/failures/errors. The alternative is deleted.

### 2.8 PR-008 — exhaustive ACL negatives + foreign row-lock probes

**Defect:** S1 ACL test omitted foreign row-lock denial, PUBLIC schema/function zeros, `evidence_role` USAGE-not-CREATE, and exhaustive S1/S2 role negatives.

**Plan change (§7.3 `v008OwnerPublicRuntimeAndCrossModuleAclsAreExact` + §10 helper table):**
- PUBLIC zeros via the PA7 `aclexplode` / `grantee = 0` expansion of `coalesce(n.nspacl, acldefault('n', n.nspowner))` (and table/column/function ACLs). **Forbidden:** `has_schema_privilege('public', ...)` — PUBLIC is ACL pseudo-grantee OID 0, not a login.
- `evidence_role`: USAGE but not CREATE on `evidence`.
- Every S1/S2 role (full `S1_ROLES` + `V004_ROLES`): no USAGE/CREATE on `evidence`, no EXECUTE on the five `evidence.*` trigger functions, no evidence table/column privilege.
- **Foreign row-lock negatives:** as `evidence_role`, `SELECT <granted columns> ... FOR SHARE` and `... FOR UPDATE` on `subject_number.business_subject`, `ivr.call_record`, `ivr.inbound_session`, `entitlement_metering.usage_reservation` must fail `42501`.
- Exhaustive per-table/column negatives on the four foreign parents; keep platform/audit denials.
- Helper table now names `expectCommitFailure`, the `aclexplode` PUBLIC pattern, and the `FOR SHARE`/`FOR UPDATE` 42501 pattern with source line numbers.

### 2.9 PR-009 — valid fixtures + complete 39-check map + split rollback witnesses

**Defect:** S2 method 4's rollback fixture inserted artifact/event/Outbox without an `evidence_record` (FK/cycle reject it, so T-S3-12 would pass for the wrong reason); 18 of 39 checks had no named probe.

**Plan change (S2):**
- **Recording-only rollback** lives in `v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops`: `media_object` + `recording` only, force failure before COMMIT, zero survivors. No artifact/event/Outbox.
- **T-S3-12 full valid evidence/artifact/Outbox rollback** lives solely in `v008CreationCycleBothOrdersAndTerminalLifecycleBackstops`: record + creation event + `ORIGINAL` artifact + simulated Outbox row (all FK/cycle-satisfying), force failure before COMMIT, zero survivors.
- **Valid fixture prerequisites** stated explicitly: parent rows → `media_object` → record/event pair in the tested order → at most one `ORIGINAL` artifact. Artifact/event without an `evidence_record` is an invalid fixture.
- **39-check witness map** (plan §9 S2): every one of the 9 media_object + 11 recording + 13 evidence_record + 1 evidence_artifact + 5 evidence_event checks has exactly one named positive or negative probe (savepoint-isolated `23514` with exact constraint name).
- `occurred_at` default witness moved to the event-bearing method.

---

## 3. CONCRETE REVIEWABLE blocked-decision section (PR-001 / PR-005)

> This section is the deliverable for the two findings the controller accepted as **needs-more-evidence / frozen-design conflicts**. It does **not** select new design behavior. It does **not** claim the conflicts are fixed. The plan stays **not-ready** until the controller/user chooses.

### 3.1 PR-005 — deferred FK `ON UPDATE`/`ON DELETE` action: RESTRICT vs NO ACTION

> **Correction (same gate, 2026-10-30):** the earlier text wrongly claimed deferred RESTRICT parent actions evaluate at COMMIT and are observationally identical to deferred NO ACTION. Controller-executed PostgreSQL 16.15 probe (transient container `/tmp/pa8-fk-probe.z5lJbH/result.log`) proves the opposite: RESTRICT parent DELETE/UPDATE is immediate/nondeferrable even on a `DEFERRABLE INITIALLY DEFERRED` constraint; NO ACTION parent-delete defers to COMMIT; INSERT deferral is unchanged. All equivalence claims are removed. BD-1/BD-2 remain blocked; recommended decisions unchanged.

#### 3.1.1 Frozen contradiction (exact lines)

| Source | Text | PostgreSQL effect if executed as written |
| --- | --- | --- |
| doc-14 **§5.5** (`:2613`) | "All S3 FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT`; no cascade exists." | If this governs, both deferred cycle FKs must be created with `ON UPDATE RESTRICT ON DELETE RESTRICT`. |
| doc-14 **§11.5** step 7 (`:4249`) | `ALTER TABLE evidence.evidence_record ADD CONSTRAINT fk_evidence_source_event FOREIGN KEY (source_event_domain, source_event_id) REFERENCES evidence.evidence_event(event_domain, event_id) DEFERRABLE INITIALLY DEFERRED;` | No `ON UPDATE`/`ON DELETE` clause → PostgreSQL default **`NO ACTION`** (`confupdtype='a'`, `confdeltype='a'`). |
| doc-14 **§11.5** step 7 (`:4250`) | `ALTER TABLE evidence.evidence_event ADD CONSTRAINT fk_evidence_event_record FOREIGN KEY (evidence_id) REFERENCES evidence.evidence_record(id) DEFERRABLE INITIALLY DEFERRED;` | Same: default **`NO ACTION`**. |
| doc-14 §5.5 table rows (`:2623`, `:2626`) | Mode column says `DEFERRABLE_FK`; "Creation timing" says `DEFERRABLE INITIALLY DEFERRED`; **no action column**. | Action unspecified at the row level. |
| Pre-fix plan §3.7 / §7.3 | Reproduces the §11.5 statements verbatim; asserts RESTRICT only for the 8 ordinary FKs. | Silently leaves the 2 deferred actions unasserted. |

The 8 **ordinary** S3 FKs are unambiguous: every one of them is written `ON UPDATE RESTRICT ON DELETE RESTRICT` in §4.39–§4.43 and in the V007 sibling pattern. The conflict is **only** about the two deferred cycle FKs.

#### 3.1.2 Alternative A — §11.5 exact DDL governs (NO ACTION)

**Catalog:**
- `fk_evidence_source_event`: `confupdtype='a'`, `confdeltype='a'`, `condeferrable=true`, `condeferred=true`.
- `fk_evidence_event_record`: same.

**Insert behavior (both alternatives):** unchanged. Deferring the FK check is what permits either row-insertion order until COMMIT. RESTRICT vs NO ACTION does not change insert-time deferral.

**Parent-update / parent-delete behavior (the only real difference):**

**Controller-executed runtime proof** (PostgreSQL 16.15, dedicated transient container at `/tmp/pa8-fk-probe.z5lJbH/result.log`; container auto-removed; **not author-executed**). Setup: parent `pa`/`pr` and child `ca`/`cr REFERENCES ... DEFERRABLE INITIALLY DEFERRED`, committed rows, then `BEGIN; DELETE parent; SELECT marker; COMMIT`:
- `ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED` (`ca_pid_fkey`): DELETE succeeds and the marker prints; COMMIT raises `23503`. The NO ACTION parent-delete check **defers**.
- `ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED` (`cr_pid_fkey`): DELETE **immediately** raises `23503`; the marker then raises `25P02` and the transaction ROLLBACK. The RESTRICT parent-delete action is **immediate and nondeferrable** even though the constraint is `DEFERRABLE INITIALLY DEFERRED` and FK insertion checks are still deferrable.

| Operation | NO ACTION (constraint `DEFERRABLE INITIALLY DEFERRED`) | RESTRICT (constraint still `DEFERRABLE INITIALLY DEFERRED`) |
| --- | --- | --- |
| INSERT child / FK insertion check | Deferred to COMMIT. **Unchanged between alternatives.** | Same: deferred to COMMIT. INSERT deferral unchanged. |
| UPDATE parent key (`evidence_event.event_id` / `evidence_record.id`) while a child row references it | NO ACTION parent-update check is deferrable: evaluated at COMMIT; fails `23503` if the child would dangle. | RESTRICT parent-update action is **nondeferrable**: fails `23503` immediately at `executeUpdate`. |
| DELETE parent (`evidence_event` row referenced by `evidence_record.source_event_*`) | Deferred to COMMIT. `executeUpdate` succeeds; COMMIT fails `23503`. | Immediate. `executeUpdate` fails `23503` (`cr_pid_fkey` class); transaction aborted (`25P02` on the next statement). |
| DELETE parent (`evidence_record` row referenced by `evidence_event.evidence_id`) | Same as above. | Same as above. |
| Difference visible to tests? | **Yes.** Parent-delete/parent-update probes observe commit-time `23503` via `expectCommitFailure`. | **Yes.** The same probes observe immediate `23503` via `expectSqlFailure`. The two alternatives are **not** observationally identical. |

**Key fact (no equivalence claim):** INSERT deferral is unchanged between alternatives. Parent-delete and parent-update timing is **not** identical: NO ACTION defers the parent-action check to COMMIT; RESTRICT runs the parent action immediately even on a `DEFERRABLE INITIALLY DEFERRED` constraint (controller probe above). Any V008 test that deletes or updates a deferred-FK parent distinguishes Alternative A from Alternative B. Child rows in V008 are append-only (no cascade; this gate's tests do not hard-delete events/records as ordinary children), but a parent-delete probe of the deferred FK is still specified and is action-sensitive.

**Consequence of choosing A:** §5.5's sentence "All S3 FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT`" becomes **false** and must be amended (see §3.1.4). Plan S1 would assert `confupdtype='a'`/`confdeltype='a'` for the two deferred FKs.

#### 3.1.3 Alternative B — §5.5 prose governs (RESTRICT on the deferred FKs)

**DDL change required:** add `ON UPDATE RESTRICT ON DELETE RESTRICT` to both §11.5 ALTER statements.

**Catalog:**
- both deferred FKs: `confupdtype='r'`, `confdeltype='r'`, `condeferrable=true`, `condeferred=true`.

**Insert behavior:** unchanged (deferral is independent of RESTRICT/NO ACTION).

**Parent-update / parent-delete:** RESTRICT parent actions are **nondeferrable**. Even with `DEFERRABLE INITIALLY DEFERRED`, DELETE/UPDATE of a referenced parent fails `23503` immediately (controller-executed PostgreSQL 16.15 probe; §3.1.2). INSERT deferral is unchanged. This does **not** match Alternative A's commit-time parent-delete failure. One additional documented nuance: PostgreSQL's RESTRICT is specified to refuse a parent delete even when the referencing rows are deleted in the same command; NO ACTION would allow that command. No V008 test deletes the child and parent together.

**Consequence of choosing B:** §11.5's exact statements are incomplete and must be amended. Plan S1 would assert `confupdtype='r'`/`confdeltype='r'` and the implementer would add the action clauses.

#### 3.1.4 Recommended minimal reconciliation

**Recommendation: Alternative A (§11.5 exact DDL governs; deferred FKs are default NO ACTION), with a one-sentence §5.5 amendment.**

Rationale:
1. §11.5 is the **executable** creation-order contract. The plan and every prior wave follow the exact-ALTER pattern. Changing the DDL to match prose is a larger edit than correcting one over-broad sentence.
2. Parent-delete/parent-update timing **differs** (controller probe, §3.1.2): NO ACTION defers to COMMIT; RESTRICT is immediate. Insert-order and creation-cycle tests are unchanged. Choosing A keeps the §11.5 DDL literal and confines the amendment to one over-broad prose sentence; choosing B would require DDL edits and an immediate (`expectSqlFailure`) parent-delete probe shape instead of `expectCommitFailure`.
3. PostgreSQL's default for an FK with no action clause is NO ACTION. The §11.5 statements, taken literally, already produce a legal, deferred, non-cascading constraint.
4. §5.5's sentence is best read as "no cascade exists anywhere in S3; all *ordinary* S3 FKs are RESTRICT" — the sentence overreaches onto the two DEFERRABLE_FK rows, which §5.5 itself lists separately with no action column.

**Precise design lines needing amendment (if the controller accepts the recommendation):**

| File:line | Current | Required amendment |
| --- | --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md:2613` | "All S3 FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT`; no cascade exists." | "All **ordinary** S3 FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT`. The two deferred cycle FKs (§5.5 rows 7 and 10; §11.5 step 7) use PostgreSQL default `NO ACTION` and are `DEFERRABLE INITIALLY DEFERRED`. No cascade exists." |
| `docs/14-production-data-model-and-flyway-schema-spec.md:4249-4250` | exact ALTERs (no action clauses) | **unchanged** under Alternative A. Under Alternative B, append `ON UPDATE RESTRICT ON DELETE RESTRICT` to both. |
| plan §3.7 / §7.3 / §8.1 | "the 2 deferred are `DEFERRABLE INITIALLY DEFERRED`" (action unasserted) | After adjudication, assert `condeferrable=true`, `condeferred=true` **and** the chosen `confupdtype`/`confdeltype` (`'a'`/`'a'` under A, `'r'`/`'r'` under B). |

**If the controller chooses Alternative B instead:** amend doc-14 §11.5 `:4249-4250` to add the RESTRICT clauses; leave §5.5 `:2613` unchanged; pin `confupdtype='r'`/`confdeltype='r'` in plan S1. INSERT deferral and both-orders inserts still hold. Parent-delete probes become **immediate** `expectSqlFailure("23503", ...)` — do **not** use `expectCommitFailure` for RESTRICT parent actions.

**What this fix does *not* do:** it does not edit doc-14, does not pick an action in the plan's S1 FK assertions, and does not claim either alternative is accepted.

### 3.2 PR-001 — deferred FK checks vs creation-cycle trigger precedence

#### 3.2.1 Frozen contradiction (exact lines)

| Source | Text | Physical problem |
| --- | --- | --- |
| doc-14 T-S3-09 (`:4734`) | "Insert an evidence record with no matching `EVIDENCE_CREATED` event and commit. → Commit fails with `EVIDENCE_CREATION_CYCLE_VIOLATION`" | A record with **no event row at all** leaves `fk_evidence_source_event` unsatisfied. The internal RI check trigger `RI_ConstraintTrigger_c_*` is queued before the user trigger `trg_evidence_record_creation_cycle` (PostgreSQL queues same-event triggers in trigger-name order; uppercase `R` sorts before lowercase `t`). COMMIT therefore raises `23503` naming `fk_evidence_source_event`, **not** `EVIDENCE_CREATION_CYCLE_VIOLATION`. |
| doc-14 T-S3-11 (`:4736`) | "Insert a creation event with the wrong `event_type`, a different owning `evidence_id`, or a source event identity that does not equal the record source. → Commit fails … with `EVIDENCE_CREATION_CYCLE_VIOLATION`" | "Different owning `evidence_id`" with no such record → `23503` on `fk_evidence_event_record`. "Source event identity that does not equal the record source" is insertion-order dependent (see §3.2.2). Only the "wrong `event_type`" and "wrong-owner with both parents present" shapes can be forced to the cycle error. |
| doc-14 §10.22 (`:4043-4044`) | trigger predicates raise `EVIDENCE_CREATION_CYCLE_VIOLATION` | Predicates are correct **when they fire**. The issue is which enforcement runs first. |
| Pre-fix plan §3.7 / §4 / §9 | promised `EVIDENCE_CREATION_CYCLE_VIOLATION` for "missing, duplicate, wrong-type, wrong-evidence-owner, or mismatched" | Over-promises: four of those five shapes cannot produce that error for every construction. |

#### 3.2.2 Classification (required; do not promise one error for all cases)

| Class | Exact construction | Expected error | Which enforcement |
| --- | --- | --- | --- |
| **FK-satisfied cycle contradiction** | Insert `evidence_record` + a **decoy `evidence_event`** with `event_type <> 'EVIDENCE_CREATED'` (e.g. `EVIDENCE_SOLIDIFIED`) and `(event_domain, event_id) = (NEW.source_event_domain, NEW.source_event_id)`. Both deferred FKs are satisfied. | COMMIT fails **`EVIDENCE_CREATION_CYCLE_VIOLATION`** from `trg_evidence_record_creation_cycle` (and/or `trg_evidence_event_creation_cycle`). | Creation-cycle constraint trigger. This is the construction that satisfies T-S3-09's *intent* ("no matching `EVIDENCE_CREATED`") **without changing DB behavior**. |
| **FK-satisfied wrong-owner** | Insert record R1 + event E whose `evidence_id = R2` (R2 exists, is not R1) and `event_type = 'EVIDENCE_CREATED'`, with `(event_domain, event_id)` equal to R1's source identity (so R1's composite FK passes) **or** equal to R2's source identity (so E's FK passes). Pin one variant. | COMMIT fails **`EVIDENCE_CREATION_CYCLE_VIOLATION`** from the trigger whose predicate checks ownership. | Creation-cycle constraint trigger. |
| **Genuinely absent event parent** | Insert `evidence_record` with `source_event_*` pointing at **no** `evidence_event` row. | COMMIT fails **`23503`** naming `fk_evidence_source_event`. **Not** the cycle error. | Deferred RI check (`RI_ConstraintTrigger_*` fires first). |
| **Genuinely absent record parent** | Insert `evidence_event` with `evidence_id` pointing at **no** `evidence_record`. | COMMIT fails **`23503`** naming `fk_evidence_event_record`. | Deferred RI check. |
| **Duplicate creation event** | Second `EVIDENCE_CREATED` for one `evidence_id`. | **Immediate `23505`** on `uq_evidence_created_event` (partial unique index). Not a commit-time error at all. | Partial unique index. |
| **Mismatched identity (both rows present)** | Record's `source_event_*` ≠ event's `(event_domain, event_id)`, both parents exist. | Insertion-order dependent: **event-first** → the event-side trigger sees the mismatch and raises `EVIDENCE_CREATION_CYCLE_VIOLATION`; **record-first** → the record-side composite FK is unsatisfied and raises `23503`. Pin one order (prefer event-first) **or** accept `{23503, EVIDENCE_CREATION_CYCLE_VIOLATION}` with an explicit rationale. Do **not** invent a third SQLSTATE. | Depends on order. |
| **Parent delete of a deferred-FK event/record row** | Delete a parent whose only child is a deferred FK row. | **Action-sensitive (PR-005):** deferred NO ACTION → `executeUpdate` succeeds, COMMIT fails `23503` (`expectCommitFailure`); deferred RESTRICT → immediate `23503` at `executeUpdate` (`expectSqlFailure`). Controller probe: §3.1.2. | NO ACTION: deferred RI check at COMMIT. RESTRICT: immediate RI check. |

#### 3.2.3 Exact constructions the plan now specifies (executable without changing DB behavior)

1. **Decoy non-creation event (primary T-S3-09 witness):**
   ```sql
   -- parents + media_object as usual
   INSERT INTO evidence.evidence_record (id, subject_id, source_type, source_ref_id,
       source_event_domain, source_event_id, ..., status, snapshot, snapshot_sha256)
   VALUES (:rec, :subject, 'CALL_RECORDING', :recording_ref,
       'EVIDENCE', :evt_id, ..., 'PENDING', :snap, :snap_sha);
   INSERT INTO evidence.evidence_event (id, evidence_id, event_domain, event_id,
       event_type, payload_redacted, payload_sha256)
   VALUES (:evt, :rec, 'EVIDENCE', :evt_id,
       'EVIDENCE_SOLIDIFIED', '{}', :payload_sha);  -- NOT EVIDENCE_CREATED
   COMMIT;  -- expectCommitFailure("P0001", "EVIDENCE_CREATION_CYCLE_VIOLATION")
   ```
   Both deferred FKs pass (the composite FK matches `('EVIDENCE', :evt_id)`, the reverse FK matches `:rec`). The record-side creation-cycle trigger's predicate ("exactly one `EVIDENCE_CREATED` for `NEW.id`") fails → `EVIDENCE_CREATION_CYCLE_VIOLATION`. Zero survivors.

2. **Genuinely absent parent (T-S3-09 alternative shape):**
   ```sql
   INSERT INTO evidence.evidence_record (... source_event_domain, source_event_id ...)
   VALUES (..., 'EVIDENCE', :evt_id_that_does_not_exist);
   COMMIT;  -- expectCommitFailure("23503", "fk_evidence_source_event")
   ```

3. **Duplicate creation (T-S3-10):**
   ```sql
   -- valid record + EVIDENCE_CREATED pair already committed
   INSERT INTO evidence.evidence_event (... event_type = 'EVIDENCE_CREATED' ...)
   VALUES (..., :rec, ...);  -- second one
   -- expectSqlFailure("23505", "uq_evidence_created_event", ...)  -- immediate
   ```

4. **Mismatched identity, pinned event-first (T-S3-11 shape):**
   ```sql
   INSERT INTO evidence.evidence_event (id, evidence_id, event_domain, event_id,
       event_type, ...) VALUES (:evt, :rec, 'EVIDENCE', :evt_id, 'EVIDENCE_CREATED', ...);
   INSERT INTO evidence.evidence_record (..., source_event_domain, source_event_id)
   VALUES (..., 'EVIDENCE', :different_evt_id);
   COMMIT;  -- expectCommitFailure — event-side trigger raises EVIDENCE_CREATION_CYCLE_VIOLATION
            -- (record-side FK would raise 23503 if the order were reversed)
   ```

#### 3.2.4 Commit-aware error helper (required)

The existing `expectSqlFailure` (`PostgresContainerIT.java:3626-3651`) binds a savepoint and runs `executeUpdate` only. It **cannot** observe:
- deferred FK `23503` at COMMIT,
- `EVIDENCE_CREATION_CYCLE_VIOLATION` at COMMIT,
- parent-delete failures that surface only at COMMIT.

**Required new helper (test-local, no service implementation):**

```
expectCommitFailure(connection, expectedSqlState, messageFragment, setup)
  // opens/uses a dedicated transaction
  // runs `setup` (inserts + optional extras)
  // calls connection.commit()
  // asserts SQLException.getSQLState() == expectedSqlState
  // asserts message contains messageFragment (constraint name or stable code)
  // asserts zero survivors for the fixture ids (record/event/artifact/media)
  // if commit unexpectedly succeeds: ROLLBACK and fail
```

Do **not** reuse `expectSqlFailure` for commit-time checks. Do **not** accept an arbitrary SQLSTATE substitute. If PostgreSQL 16 at implementation returns a different SQLSTATE than the one specified above for a given class, **stop and report** — do not widen the assertion.

#### 3.2.5 What still requires runtime proof vs design decision

| Item | Runtime proof (S1/S2, PostgreSQL 16) | Design decision (controller/user) |
| --- | --- | --- |
| Trigger-name ordering (`RI_ConstraintTrigger_*` before `trg_*`) | yes — observe which constraint/SQLSTATE actually fires for the genuinely-absent-parent class | no |
| Decoy-non-creation-event fixture yields `EVIDENCE_CREATION_CYCLE_VIOLATION` | yes — execute construction §3.2.3.1 | no |
| Both insertion orders commit (T-S3-08) | yes | no |
| Immediate `23505` on `uq_evidence_created_event` | yes | no |
| `expectCommitFailure` observes commit-time errors | yes | no |
| Whether T-S3-09's frozen wording ("no matching `EVIDENCE_CREATED`") is meant to cover the **genuinely-absent-parent** class or only the **FK-satisfied** class | partially — runtime shows what is physically possible | **yes.** If the frozen text demands `EVIDENCE_CREATION_CYCLE_VIOLATION` even when no event row exists, that is physically unattainable without disabling/renaming the deferred FK (forbidden). The controller must either (a) narrow T-S3-09 to the FK-satisfied class, or (b) authorize a different enforcement design (out of this gate). |
| Whether T-S3-11's "different owning `evidence_id`" / "source event identity" cases may accept `{23503, EVIDENCE_CREATION_CYCLE_VIOLATION}` | yes — run both insertion orders and record both outcomes | **yes** — the frozen text names one error for all three shapes. |
| Deferred FK action RESTRICT vs NO ACTION (PR-005) | runtime confirms catalog values and that parent-action timing **differs** (controller probe §3.1.2: NO ACTION defers parent-delete to COMMIT; RESTRICT is immediate/nondeferrable; INSERT deferral unchanged). It cannot decide which value the contract wants. | **yes** (see §3.1) |
| PostgreSQL runtime verification of any of the above | **not performed in this fix gate** | n/a |

**Rule:** this fix does not claim PostgreSQL runtime verification and does not automatically accept arbitrary SQLSTATE alternatives. A mismatch between the frozen text and physical PostgreSQL behavior is a **stop-and-report**, not a silent assertion change.

### 3.3 Combined blocking question for the controller/user

> **BD-1 (PR-005):** Which contract governs the two deferred cycle FKs' `ON UPDATE`/`ON DELETE` action?
> - **A (recommended):** §11.5 exact DDL (default `NO ACTION`). Amend doc-14 §5.5 `:2613` as in §3.1.4. Plan S1 asserts `confupdtype='a'`/`confdeltype='a'`.
> - **B:** §5.5 prose (RESTRICT). Amend doc-14 §11.5 `:4249-4250` to add the clauses. Plan S1 asserts `confupdtype='r'`/`confdeltype='r'`.
>
> **BD-2 (PR-001):** Do frozen T-S3-09/T-S3-11 require `EVIDENCE_CREATION_CYCLE_VIOLATION` for *every* missing/mismatched creation shape, or only for the FK-satisfied cycle-contradiction class?
> - **A (recommended):** narrow the frozen wording to the FK-satisfied class; genuinely-absent parents assert `23503` with the exact deferred constraint name; duplicates assert immediate `23505`. Plan's S2 classification table (§3.2.2) becomes the executable contract.
> - **B:** keep the frozen wording and treat the physically-unattainable shapes as a design defect that blocks V008 until an authorized enforcement redesign exists (out of this gate).

Until BD-1 and BD-2 are answered, the plan remains **NOT READY** and implementation is unauthorized.

---

## 4. PR state matrix (final)

| PR | State | Evidence |
| --- | --- | --- |
| PR-001 | **blocked** (BD-2) | §3.2; classification table, constructions, commit-aware helper specified; no silent SQLSTATE choice |
| PR-002 | **fixed** | §2.2; every down/R__/U008/R008/target-reverse option removed; transaction-rollback only |
| PR-003 | **fixed** | §2.3; six must-change locations + `V008_ROLES` constant rule |
| PR-004 | **fixed** | §2.4; migration-identity ALTER FKs; no inline cross-schema FK; no REFERENCES grant |
| PR-005 | **blocked** (BD-1) | §3.1; both alternatives analyzed; recommendation A; precise doc-14 lines named |
| PR-006 | **fixed** | §2.6; five named service functions; SECURITY DEFINER count = 5; no `pg_proc` = 5 claim |
| PR-007 | **fixed** | §2.7; 54 then 57 source=CI=XML mandated; alternative deleted |
| PR-008 | **fixed** | §2.8; `aclexplode`/`grantee=0` PUBLIC (not `has_schema_privilege('public',…)`); foreign row-lock 42501 probes; exhaustive role negatives |
| PR-009 | **fixed** | §2.9; recording-only vs T-S3-12 split; valid fixture prerequisites; complete 39-check map |

---

## 5. Actual checks performed in this fix gate

| Check | Method | Result |
| --- | --- | --- |
| Migration directory inventory | Glob `backend/src/main/resources/db/migration/*` | V001–V007 only |
| V007 FK ALTER pattern | Read `V007__activation_and_consent.sql:248-313` | all FKs after `RESET ROLE`; RESTRICT on every ordinary FK |
| SECURITY DEFINER count/names | Grep `SECURITY DEFINER` + `CREATE FUNCTION` in V001/V006 | 4 + 1 = 5; `issue_first_trial` not unique |
| Global function list | Read `PostgresContainerIT.java:6292-6463` | 35 functions, 33 triggers, all-schema `containsExactly` |
| History block | Read `PostgresContainerIT.java:1184-1252` | rows 001–007, then `next()` false |
| V003 schema-absence | Read `PostgresContainerIT.java:17530-17548` | `IN ('evidence','sharing')` is zero |
| Role constants | Read `PostgresContainerIT.java:67-82`, grep `S1_ROLES`/`V004_ROLES` call sites | shared across many matrices |
| `expectSqlFailure` body | Read `PostgresContainerIT.java:3626-3651` | savepoint + `executeUpdate` only |
| PUBLIC ACL pattern | Grep `aclexplode`/`grantee` in `PostgresContainerIT.java` | PA7 pattern uses `grantee = 0`; file comment confirms pseudo-grantee OID 0 |
| Foreign row-lock pattern | Grep `FOR UPDATE`/`FOR SHARE` in `PostgresContainerIT.java` | 42501 probes at :2664-2741 |
| CI expected-set | Grep `.github/workflows/ci.yml` | exact-name-set + count label `PA7 V007 S2 test count (51)` |
| doc-14 §5.5 vs §11.5 | Read `docs/14-...:2605-2634`, `:4245-4252` | RESTRICT prose vs action-less ALTERs |
| doc-14 T-S3-08…12 | Read `docs/14-...:4725-4737` | frozen wording captured verbatim |
| doc-14 forward-only | Read `docs/14-...:4203`, `:4768-4781` | "no destructive rollback"; "V001-V009 forward-only" |
| Plan SHA (pre-fix) | reviewer artifact | `0423a6acea7c130a5df63b6903a592fc275147a6a7d415bb236b6335d8416644` |
| RESTRICT vs NO ACTION parent-action timing | **controller-executed** PostgreSQL 16.15 probe (`/tmp/pa8-fk-probe.z5lJbH/result.log`; transient container; not author-executed) | NO ACTION defers parent-delete to COMMIT (`23503` at COMMIT); RESTRICT is immediate (`23503` at `executeUpdate`, then `25P02`); INSERT deferral unchanged; UPDATE RESTRICT nondeferrable |
| Writes performed | this gate | exactly two files (plan + this fix). No source/design/controller/CI/README/Git mutation. |
| PostgreSQL / Flyway / Maven / Docker | — | **not run by the author** (document-only fix gate). Controller probe above only; not re-run here. |

---

## 6. Residuals (unchanged owners/destinations unless noted)

| Residual | Classification | Destination |
| --- | --- | --- |
| BD-1 deferred-FK action (PR-005) | **blocking design decision** | controller/user; doc-14 §5.5 `:2613` or §11.5 `:4249-4250` amendment |
| BD-2 T-S3-09/T-S3-11 SQLSTATE wording (PR-001) | **blocking design decision** | controller/user; doc-14 `:4734`/`:4736` wording amendment or enforcement redesign |
| Trigger firing order / commit-time SQLSTATEs not executed | required implementation evidence | S1/S2 container tests on PostgreSQL 16 with `expectCommitFailure` |
| Final-state global catalogs can regress if updated inconsistently | test-maintenance risk | S1 implementation; `V008_ROLES` kept separate from `S1_ROLES`/`V004_ROLES` |
| Cross-module row-lock denial omitted or table-only | test gap | S1 ACL implementation; `FOR SHARE` + `FOR UPDATE` on all four foreign parents |
| Recording/evidence service orchestration absent | intentional application-service boundary | later evidence/IVR/media service work units; not PA8 |
| Q11 WORM/storage retention, Q22 ticket TTL, other production inputs | deferred production inputs | existing doc-14 owners/destinations |
| Production role provisioning / runtime switching unproven | deferred production wiring | Platform/Database/Security production-readiness |
| V009 sharing absent | future-wave scope | PA9; preserve no-V009 assertions |
| PostgreSQL runtime verification of every SQLSTATE/constraint name | required at implementation | S1/S2; stop-and-report on mismatch |

---

## 7. Files written by this fix gate

| File | Action |
| --- | --- |
| `docs/gateflow/pa8-v008-evidence-foundation-plan.md` | **modified** (PR-002/003/004/006/007/008/009 fixes; PR-001/005 marked blocked; not-ready banner) |
| `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-1.md` | **created** (this artifact) |

No other file was written. No Git state was changed. No delegation was performed.

---

## 8. Fix-gate completion

1. **Verdict:** `blocked` — plan defects PR-002/003/004/006/007/008/009 are fixed; PR-001/PR-005 remain open design contradictions (BD-1/BD-2). Plan is **NOT READY**.
2. **Artifacts:** `docs/gateflow/pa8-v008-evidence-foundation-plan.md`, `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-1.md`.
3. **Exact files changed:** the two above; nothing else.
4. **Validation actually performed:** see §5. Document-only author gate; one controller-executed PostgreSQL 16.15 FK probe (§3.1.2) supplies the RESTRICT/NO ACTION timing facts. No author-run PostgreSQL/Maven/Docker.
5. **Slice count:** unchanged (2). Rationale unchanged (catalog/ACL/FK vs lifecycle/concurrency; no late ALTER_FK, no function wave).
6. **Blocking questions:** BD-1 (PR-005), BD-2 (PR-001) — see §3.3.
7. **Residual owners:** see §6.

**STOP.** No implementation, no next gate, no commit/push/PR/merge.
