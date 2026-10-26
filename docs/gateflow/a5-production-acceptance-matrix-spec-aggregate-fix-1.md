# A5 aggregate deepreview bounded fix 1 (STATUS: COMPLETE)

- Work unit: `A5-production-acceptance-matrix-spec`
- Gate: `aggregate deepreview fix` 1
- Task label: `a5-production-acceptance-matrix-aggregate-fix1-ds-20261026-01`
- Provider/model: deepseek-flash[1m]
- Date: 2026-10-26
- Aggregate review: `docs/reviews/code-review-20261026-194942.md`
- Accepted plan commit: `bc6ab32a3f0fe3fec0d3b8504b45d8bf0184a058`
- This artifact: `docs/gateflow/a5-production-acceptance-matrix-spec-aggregate-fix-1.md`

## Accepted finding

`F-AGG-01` (Low, bookkeeping), the only finding in the aggregate deepreview:
`docs/gateflow/a5-production-acceptance-matrix-spec-s1-implementation.md:6` cited the
non-existent full SHA `bc6ab32f12b0301b5e29b23af419d9a4fb9a7ea4` as the accepted plan
checkpoint.

Direct evidence recorded by the review (`code-review-20261026-194942.md:15-19`) and
re-verified this session:

- `git cat-file -t bc6ab32f12b0301b5e29b23af419d9a4fb9a7ea4` -> `fatal: git cat-file: could
  not get object info` (not a git object).
- `git cat-file -t bc6ab32a3f0fe3fec0d3b8504b45d8bf0184a058` -> `commit`
  (`gateflow: accept A5 production acceptance matrix plan`).

Only the 7-character prefix `bc6ab32` matches the real accepted-plan commit; the stale full
SHA was a bad expansion of that prefix.

## Exact change

One line, one citation, in one file:

```text
before: - Base commit: `e36b350` (accepted plan checkpoint `bc6ab32f12b0301b5e29b23af419d9a4fb9a7ea4`)
after:  - Base commit: `e36b350` (accepted plan checkpoint `bc6ab32a3f0fe3fec0d3b8504b45d8bf0184a058`)
```

No other text, line, table, hash, counter, path list or report section was changed.

## Changed files (exactly two allowed paths)

1. `docs/gateflow/a5-production-acceptance-matrix-spec-s1-implementation.md` (modified: line 6
   citation only)
2. `docs/gateflow/a5-production-acceptance-matrix-spec-aggregate-fix-1.md` (created: this
   artifact)

No other repository path was modified or created by this task. The pre-existing unrelated dirty
working tree (screenshots, prototypes, fixtures, docs, reviews, control document, `.codex-tasks/`,
`docs/reviews/plan-review-20261021-205118.md`) is unchanged and out of scope.

## Validation results

| Check | Result |
| --- | --- |
| `git cat-file -t bc6ab32a3f0fe3fec0d3b8504b45d8bf0184a058` | `commit` |
| Stale SHA `bc6ab32f12b0301b5e29b23af419d9a4fb9a7ea4` in the S1 implementation report | absent (grep: no match) |
| Correct SHA present in the intended citation | yes, line 6: `accepted plan checkpoint bc6ab32a3f0fe3fec0d3b8504b45d8bf0184a058` |
| `git diff --check -- <both allowed paths>` | exit `0`, no output |
| Task-scoped change count | exactly two paths: one line modified + this artifact created |
| Validator / matrix / fixture / docs-19 / A0-A4 behavior | untouched; no generated artifact or evidence input changed |

Validator, A1-A4 and JSON-parse commands were intentionally not re-run: this fix changes no
executable input, model, matrix, fixture or contract, and a document-only citation fix cannot
alter any of those results. The accepted S1/S2 and aggregate evidence remains as recorded in the
accepted artifacts.

## Docs decision

Documentation-only bookkeeping correction. The S1 implementation report is the authoritative
record of the S1 gate and must cite resolvable repository objects; correcting its checkpoint
citation restores that rule without changing any documented contract, counter or conclusion.
`docs/19-production-acceptance-matrix.md`, `docs/README.md`, the plan, the control document and
all review artifacts are unchanged: no documentation content or index row is affected.

Historical review artifacts that quoted the earlier citation (for example
`docs/reviews/code-review-20261026-175658.md:5`) are immutable evidence and were deliberately not
rewritten. The review itself (`docs/reviews/code-review-20261026-194942.md`) was not modified,
per the fix brief.

## Residual risks and uncovered areas (classified, with owner/destination)

- `LOW` — Aggregate review closeout condition 2 (fresh full plan §10 suite: S1 x2, S2 x2,
  `--check-doc-links`, `node --check`, A1-A4, five-JSON parse in a permissioned session, frozen
  hashes `085752ae…`/`810029be…`) is unrelated to this bookkeeping fix and remains open.
  Owner: controller. Destination: A5 aggregate closeout / draft-PR readiness.
- `LOW` — The stale 40-hex string intentionally remains in immutable historical review artifacts
  and excluded `.codex-tasks/` scope; it was not a new defect and rewriting it is out of scope.
  Owner: Gateflow archive policy. Destination: none by design.
- `NOT_RUN` — Runtime, carrier, IVR-line, cloud, database, manual and load execution were not
  performed and are not claimed. Owner: production acceptance. Destination: future runtime
  acceptance gates (`productionAccepted=false`).
- `NOT_RUN` — Validators, A1-A4 regressions and JSON parsing were not re-executed for this
  document-only fix (see Validation results). Owner: controller/closeout. Destination: the
  permissioned closeout run listed above.

## Completion status

`COMPLETE` — `F-AGG-01` fixed by replacing only the stale plan-checkpoint SHA with the verified
commit `bc6ab32a3f0fe3fec0d3b8504b45d8bf0184a058`; exactly the two allowed paths were changed;
all required validations passed. This artifact does not assert runtime, carrier, IVR-line, cloud,
database or production acceptance.

- Artifact path: `docs/gateflow/a5-production-acceptance-matrix-spec-aggregate-fix-1.md`
- Stop condition: no staging, commit, push, PR/issue action, merge, re-review entry, gate change
  or further fix was performed.
