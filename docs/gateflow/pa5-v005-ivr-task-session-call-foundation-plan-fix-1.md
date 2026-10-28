# PA5 V005 Plan Fix 1 - Authorized `call_record.created_at` Errata

- Task label: `pa5-v005-planfix-ds-20261028-01`
- Runtime / provider: Claude Code harness / `ds` (DeepSeek `deepseek-flash`), plan/design only
- Work unit: `PA5-v005-ivr-task-session-call-foundation`
- Gate: `fix` (plan/design only); no implementation and no entry into another gate
- Base/head: `4006e8e09c39e4527cb543f03646a30073d1e1fa`
- Branch: `codex/pa5-v005-ivr-task-session-call-foundation`
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Preserved Git env: `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call4`, `GIT_WORK_TREE` = workspace
- Inputs read: PA5 section of `docs/gateflow/project-execution-control.md`; `docs/14-production-data-model-and-flyway-schema-spec.md` §4.1, §4.23-§4.28, §11.4, §13.2.1; `docs/12` references carried by the plan; `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md`; `docs/reviews/plan-review-20261028-142515.md`; migration directory and V001-V004 Git blobs.
- Write scope: only `docs/14` (bounded call_record errata/totals), the PA5 plan, and this artifact. No migration, Java, CI, README, controller, review artifact, runner/prompt file or Git metadata was edited; no stage/commit/push/PR/approve/merge.

## 1. User decision (binding)

The user explicitly selected **option B** to close accepted `F-PA5-PR-01`/`F-PA5-PR-02`:

- Add `ivr.call_record.created_at timestamptz NOT NULL DEFAULT now()`.
- Preserve the frozen `ix_call_record_subject_time(subject_id, ended_at, created_at)` literally.
- Treat this as **authorized schema errata**, not a semantic-preserving transcription correction.
- This is the **only** authorized contract change.
- `ivr.call_record` now has 23 columns; the six-table V005 total is **103** (23/17/23/11/14/15).
- Defaulted columns total **17**; `ivr.call_record` contributes 3 (`status`, `created_at`, `revision`).
- All other counts, keys, checks, guards, grants and V001-V004 remain unchanged.
- `created_at` is insert-only under the existing runtime `UPDATE` allowlist; no new guard, check or grant is invented.

## 2. Changed paths

| Path | Change |
| --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | §4.1 catalog field-6 list adds `created_at`; §4.25 dictionary adds the `created_at` row; bounded authorized-errata note added in §4.25; §13.2.1 `need["ivr.call_record"]=23` and the S2/cumulative column totals 307→308 and 428→429, 522→523; the frozen three-column index line is unchanged. |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md` | All current inventory/default/assertion references, the exact column list, the approved index, F-PA5-PLAN-01 resolution, F-PA5-PR-01/02 errata classification, prerequisites, risk table, §12 historical-evidence labeling and §13 completion status updated; the two-column recommendation is withdrawn as an operative instruction. |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan-fix-1.md` | This new bounded fix record. |

Explicitly unchanged: V001-V004 migrations; V005 does not exist yet; Java, POM, CI, `backend/README.md`, `docs/12`, the controller record, the plan-review artifact, `.pa5-*` runner/prompt files and Git metadata.

## 3. Exact totals before / after

| Quantity | Before (reviewed plan) | After (authorized errata) |
| --- | ---: | ---: |
| `ivr.call_record` columns | 22 | **23** |
| Six-table V005 columns | 102 (23/17/22/11/14/15) | **103 (23/17/23/11/14/15)** |
| Defaulted columns | 16 | **17** |
| `ivr.call_record` defaults | 2 (`status`, `revision`) | **3 (`status`, `created_at`, `revision`)** |
| Named checks | 73 | 73 |
| PKs / ordinary unique constraints / partial unique indexes | 6 / 8 / 6 | 6 / 8 / 6 |
| Unique keys / total indexes | 14 / 31 | 14 / 31 |
| Immediate FKs / NO_FK roots / V006-deferred FK | 15 / 2 / 1 | 15 / 2 / 1 |
| Trigger functions / triggers | 5 / 5 | 5 / 5 |
| `id` defaults / sequences | 0 / 0 | 0 / 0 |
| Methods | 31 → 33 → 38 | 31 → 33 → 38 |
| `ix_call_record_subject_time` | two-column recommendation | **frozen three-column `(subject_id, ended_at, created_at)`** |

`created_at` has no additional check, key, FK, guard, trigger or grant; it is outside `ivr_role`'s frozen `UPDATE` column allowlist.

## 4. Finding dispositions

| Finding | Status | Disposition |
| --- | --- | --- |
| `F-PA5-PR-01` (blocking, frozen index vs 22-column dictionary) | **Closed** | User selected option B; `created_at` is added by explicit schema authority; `docs/14` and plan totals are amended; the silent two-column substitution is removed. |
| `F-PA5-PR-02` (misclassified transcription) | **Closed** | Plan §4.3 now separates the one genuine transcription correction (`F-PA5-PLAN-02` JSONB grouping) from the authorized schema errata and states that the errata is the only authorized contract change. |
| `F-PA5-PLAN-01` | **Resolved** | Risk row and §13 rewritten as resolved; recommends option B outcome; no blocker remains. |
| `F-PA5-PLAN-02` | Unchanged | JSONB `payload_redacted` grouping remains a semantics-preserving executable transcription. |
| `F-PA5-PLAN-03` / `F-PA5-PLAN-04` | Unchanged | Comment-only correction and the frozen `started_at` grant/guard inconsistency remain as recorded; neither is redesigned. |
| `F-PA5-PR-03` / `F-PA5-PR-04` / `F-PA5-PR-05` | Unchanged | Accepted by the review; no plan action required. |

## 5. Validation performed vs NOT performed

Performed (read/check only):

- `git status --short` scope check: only `docs/14` (this fix), the pre-existing controller-owned `project-execution-control.md` modification, the pre-existing untracked PA5 plan/review and `.pa5-*` runner files, plus this new fix artifact.
- `git diff --check` on tracked paths: exit 0, no whitespace diagnostics.
- V001-V004 Git-blob content SHA-256 recomputed and unchanged: V001 `946a0a13...cf81045`; V002 `534a0a06...d9795d5`; V003 `1d3ffb5f...b873fb1`; V004 `a7ba7a8c...6e496edc`. Migration directory still contains only V001-V004.
- Exact text/count reconciliation: `docs/14` S2 dictionary-count program passes on the LF-normalized content (`awk-exit=0`) with `ivr.call_record=23`; the `ivr.call_record` catalog field-6 column-name set equals its dictionary column-name set at 23 columns; the other five V005 tables are unchanged.
- Trailing-whitespace scan of both edited files: zero lines.
- EOL preservation: `docs/14` remains 100% CRLF; the plan remains LF; no mixed endings introduced.
- Historical evidence preserved: plan §12 102-column probe remains explicitly labeled historical/superseded; the current 103-column variant is **not** claimed as executed.

NOT performed:

- No Maven, Docker, PostgreSQL or psql execution; no 103-column disposable probe; no live or new DB deployment.
- No CI run; no boot/fat-jar check; no implementation validation.
- No edit or validation of V005 because V005 is not authored in this gate.
- Shell `apply_patch` was not present in this environment; the harness's exact-string patch tool (Edit) was used for the bounded local edits instead.

## 6. Documentation decision

- `docs/14` is the binding design source and is amended only for the user-authorized `ivr.call_record.created_at` errata, its four affected totals, and a bounded errata note in §4.25. The frozen three-column index text is preserved verbatim.
- `docs/12` is unchanged; no new design document is introduced.
- The plan is amended so the implementation author receives a single consistent 103-column/17-default/three-column-index contract.
- README and CI updates remain implementation-gate (S1/S2) evidence work, not part of this plan fix.

## 7. Residual risks, owners and destinations

| Residual | Owner / destination |
| --- | --- |
| `call_record.created_at` owner-level immutability is service-owned; runtime is INSERT-only via the unchanged column grant and no guard is added. | IVR service wave; keep the residual note in S1/S2 artifacts. |
| The 103-column/17-default/three-column-index contract has no executable PostgreSQL evidence yet; the 102-column probe is historical/superseded. | S1 implementation + controller executable validation (PostgreSQL 16), then fresh review. |
| Closure of `F-PA5-PR-01`/`F-PA5-PR-02` is author-reported and requires independent re-review. | Controller; fresh plan re-review. |
| `F-PA5-PLAN-03` (comment-only) and `F-PA5-PLAN-04` (`started_at` grant/guard inconsistency) remain open implementation notes. | S1 test change and S1/S2 artifact risk notes. |
| Q7/Q8/Q10/Q15/Q16/Q18/Q24/Q25/Q26/Q27/Q28 and Q17/Q23 remain deferred inputs with their named owners. | Named later owners/waves; not guessed in PA5. |
| Service-owned claim/reservation atomicity, expiry housekeeping, replay/conflict classification, `CALL_IN_PROGRESS`, `UNKNOWN` reconciliation, event absorption and decision selection remain out of scope. | Later IVR/entitlement service waves. |

No unresolved contract choice remains. If a new contradiction appears, the fix gate stops rather than guessing.

## 8. Completion / report path

- This artifact is the bounded fix record; the gate response supplies the JSON-compatible completion report with status, task label/provider, artifact path, changed paths, `F-PA5-PR-01`/`F-PA5-PR-02` status, exact inventory totals, validation performed vs not performed, and residual risks with owners/destinations.
- Next gate is `re-review` after controller intake. This fix does not enter it and performs no commit, stage, push, PR, approve or merge.
