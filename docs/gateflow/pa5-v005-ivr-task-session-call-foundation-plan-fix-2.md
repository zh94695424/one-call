# PA5 V005 Plan Fix 2 - S2 Expected-Count Annotation Reconciliation

- Task label: `pa5-v005-planfix2-ds-20261028-01` (retry: `...-retry1`)
- Runtime / provider: Claude Code harness / `ds` (DeepSeek `deepseek-flash`), documentation-count only
- Work unit: `PA5-v005-ivr-task-session-call-foundation`
- Gate: `fix` (documentation/annotation only); no implementation and no entry into another gate
- Base/head: `4006e8e09c39e4527cb543f03646a30073d1e1fa`
- Branch: `codex/pa5-v005-ivr-task-session-call-foundation`
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Preserved Git env: `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call4`, `--work-tree` = workspace (the worktree `.git` file carries a Windows `D:/` path and is not used)
- Inputs read: `docs/gateflow/project-execution-control.md` (PA5 section, selectively); `docs/14-production-data-model-and-flyway-schema-spec.md` §4.1, §4.25, §13.2.1, §13.2.2, §13.9, §13.10; `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md`; `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan-fix-1.md`; `docs/reviews/plan-review-20261028-154137.md`; `docs/reviews/plan-review-20261028-154455.md`; migration directory and V001-V004 Git blobs.
- Write scope: only `docs/14` (three stale S2-total annotation occurrences) and this new artifact. No migration, Java, CI, README, `docs/12`, plan, controller record, review artifact, runner/prompt file or Git metadata was edited; no stage/commit/push/PR/approve/merge.

## 1. Binding authorization and finding

User-authorized **option B** remains binding and is not reopened:

- `ivr.call_record.created_at timestamptz NOT NULL DEFAULT now()`;
- frozen `ix_call_record_subject_time (subject_id, ended_at, created_at)` preserved literally;
- V005 totals **103 columns (23/17/23/11/14/15)**, **17** defaulted columns;
- all other counts/keys/checks/FKs/functions/triggers/grants and `31 -> 33 -> 38` methods unchanged.

Accepted finding **F-PA5-PR-06 / F-PA5-RR-01** (medium, incomplete authorized-errata propagation): after plan-fix-1, `docs/14` §13.2.1 still published `...,17,22,11,...` with `(307 total)`, and §13.9 `T-S3-01` plus the §13.10 final cumulative closure still published S2 `307` and cumulative `522`. The executable contract pieces (catalog §4.1, dictionary §4.25, `need["ivr.call_record"]=23`, plan §4.2) were already 23/103/17, so the design source was internally inconsistent on the S2 totals.

## 2. Changed paths

| Path | Change |
| --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | Three stale S2-total annotation occurrences corrected: §13.2.1 accepted-column expected counts `...,17,22,11,...` → `...,17,23,11,...` and `(307 total)` → `(308 total)`; §13.9 `T-S3-01` `522 exact columns ... (S1 121, S2 307, S3 94)` → `523 ... (S1 121, S2 308, S3 94)`; §13.10 final cumulative closure `44 tables and 522 accepted columns ... S2 23/307` → `523 ... S2 23/308` (with its PowerShell expectation `Columns=522` → `Columns=523`). No other `docs/14` line was changed by this fix. |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan-fix-2.md` | This new bounded fix record. |

## 3. Exact before / after lines

Line numbers are in the current (post-edit) file.

| Location | Before | After |
| --- | --- | --- |
| §13.2.1 expected accepted-column comparison (line 4495) | `Expected: 23 \`exact\` lines with counts \`4,17,7,6,21,11,12,23,17,22,11,14,15,11,13,10,8,14,16,12,19,6,18\` (307 total) and \`ALL_EXACT\`.` | `Expected: 23 \`exact\` lines with counts \`4,17,7,6,21,11,12,23,17,23,11,14,15,11,13,10,8,14,16,12,19,6,18\` (308 total) and \`ALL_EXACT\`.` |
| §13.9 `T-S3-01` (line 4718) | `44 dictionaries / 522 exact columns in accepted order (S1 121, S2 307, S3 94); ...` | `44 dictionaries / 523 exact columns in accepted order (S1 121, S2 308, S3 94); ...` |
| §13.10 final cumulative closure (line 4757) | `44 tables and 522 accepted columns in accepted order; S1 13/121, S2 23/307, S3 8/94.` … `expected \`Dictionaries=44\`, \`Columns=522\`, \`Errors=[]\`` | `44 tables and 523 accepted columns in accepted order; S1 13/121, S2 23/308, S3 8/94.` … `expected \`Dictionaries=44\`, \`Columns=523\`, \`Errors=[]\`` |

Deliberately preserved, not rewritten:

- `need["ivr.call_record"]=23` (§13.2.1) and the §13.2.1 S2 dictionary-total sentence `308 ... 429`, which plan-fix-1 already corrected.
- §13.2.2 cumulative sentence `... 121 S1 and 308 S2 rows ... 523` and its PowerShell expectation `Columns=523`, `S2=308`, which plan-fix-1 already corrected.
- S1 total `121`, S3 total `94`, catalog §4.1 field 6, §4.25 dictionary/errata note, the frozen three-column index, all checks/keys/FKs/functions/triggers/grants, and every unrelated total.

Retry note: on this retry the three `docs/14` corrections were already present in the working tree from the prior bounded attempt (`docs/14` SHA-256 `99785690…` vs the review-time `28146d2e…`). This retry verified them byte-for-byte and made **no further `docs/14` edit**; only this artifact was newly written.

## 4. Finding dispositions

| Finding | Status | Disposition |
| --- | --- | --- |
| `F-PA5-PR-06` (stale §13.2.1 expected counts `22`/`307`) | **Closed** | Expected counts corrected to `23`/`308`; no contract object changed. |
| `F-PA5-RR-01` (stale `22`/`307`/`522` in §13.2.1/§13.9/§13.10) | **Closed** | All three residual occurrences corrected; `docs/14` S2-total annotations now agree at 23/308/523. |
| `F-PA5-PR-01` / `F-PA5-PR-02` | Unchanged, closed | Option B remains the sole authorized contract change; no reopen. |
| `F-PA5-PR-03` / `F-PA5-PR-04` / `F-PA5-PR-05` | Unchanged, accepted | No action in this fix. |
| `F-PA5-PLAN-02` / `F-PA5-PLAN-03` / `F-PA5-PLAN-04` | Unchanged | Transcription / comment / frozen `started_at` inconsistency remain as recorded. |

## 5. Validation performed vs NOT performed

Performed (read/check only):

- Explicit-dir `git status --short`: only the pre-existing `docs/14` errata modification, the controller-owned `project-execution-control.md` modification, pre-existing untracked PA5 plan/fix-1/review/`.pa5-*` runner files, and this new artifact. No migration/Java/CI/README/`docs/12` dirty path.
- Explicit-dir `git diff --check`: exit `0`, no whitespace diagnostics.
- Targeted stale-token scan: no `307`, `522`, `S2 307`, `S2=307` or `Columns=522` remains in `docs/14`; corrected `308`/`523` occurrences confirmed at §13.2.1 (4495), §13.2.2 (4526/4550), §13.9 (4718) and §13.10 (4757).
- §13.2.1 dictionary-count program on LF-normalized content: exit `0`, no output, S2 supplied total `308` (includes `ivr.call_record=23`).
- §13.2.1 exact accepted-column-name comparison: 23 `exact` lines, `ivr.call_record exact 23 columns`, `ALL_EXACT`, counts summing to `308` (S1 `121` and S3 `94` untouched).
- V001-V004 Git-blob content SHA-256 unchanged: V001 `946a0a13…cf81045`; V002 `534a0a06…d9795d5`; V003 `1d3ffb5f…b873fb1`; V004 `a7ba7a8c…6e496edc`. Migration directory contains only V001-V004 and has no dirty path.
- EOL/whitespace: `docs/14` remains 100% CRLF (4888/4888 lines) with zero trailing-whitespace lines; this artifact is LF.

NOT performed:

- No Maven, Docker, PostgreSQL, psql or CI execution; no 103-column executable probe; no live/new DB deployment.
- No implementation, DDL, migration, Java, CI or README change; V005 is not authored in this gate.
- No stage/commit/push/PR/merge/approval and no Git metadata modification.
- The 833 KB control record was read selectively (PA5 sections) rather than in full.

## 6. Unchanged surfaces

- `docs/14` dictionary rows, §4.1 catalog field 6, `need` map, §4.25 errata note, frozen `ix_call_record_subject_time (subject_id, ended_at, created_at)`, checks, keys, FKs, functions/triggers, grants, schema contract and all non-S2 totals.
- The PA5 plan, fix-1 artifact, control record, both plan-review artifacts, runner/prompt files, `docs/12`, migrations V001-V004, Java, CI, README and Git metadata.

## 7. Residual risks, owners and destinations

| Residual | Owner / destination |
| --- | --- |
| The 103-column / 17-default / three-column-index contract still has no executable PostgreSQL evidence; the 102-column probe is historical/superseded. | S1 implementation + controller PostgreSQL 16 validation, then fresh review. |
| The corrected counts are documentation expectations only; the live §13.2.1 comparison must be rerun in S1 to prove `call_record=23`, S2 `308`, `ALL_EXACT`. | S1 implementation / controller. |
| `call_record.created_at` owner-level immutability remains service-owned; runtime is INSERT-only via the unchanged column grant with no new guard. | IVR service wave; S1/S2 artifact risk note. |
| `started_at` UPDATE-grant vs guard inconsistency (`F-PA5-PLAN-04`) remains a frozen implementation note. | S1/S2 artifact risk note; no PA5 redesign. |
| Closure is author-reported and requires independent re-review; `project-execution-control.md` remains controller-owned. | Controller; fresh plan re-review. |

No unresolved contract choice remains. If a new contradiction appears, the fix gate stops rather than guessing.

## 8. Completion status

- Status: `已修复` (author-reported); the bounded documentation-count fix is complete and internally consistent at 23/103/17 and S2 `308` / cumulative `523`, with S1 `121` and S3 `94` preserved.
- Next gate is `re-review` after controller intake. This fix does not enter it and performs no stage, commit, push, PR, approve or merge.
