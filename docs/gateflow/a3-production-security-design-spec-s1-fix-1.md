# A3 S1 bounded code-review fix 1 — accepted-plan full SHA correction

> Work unit：`A3-production-security-design-spec`
>
> Slice：S1 — sensitive-data protection, key hierarchy, rotation/recovery and historical replay
>
> Gate：`fix` (documentation correctness only; not implementation expansion or re-review)
>
> Task label：`a3-security-s1-reviewfix1-ds-20261025-01`
>
> Provider/model：Claude Code / `ds` / `deepseek-flash[1m]`
>
> Binding review artifact：`docs/reviews/code-review-20261025-222947.md`, SHA-256 `7A54C56A8A09BD3C3C54D42E19142C4C24A5A29C457CE86DCAF0C7EF7983189D` (read-only, unchanged)
>
> Stop condition：this bounded documentation fix only. No self-review, code review, staging, commit, push, PR, merge, branch change, controller update, S2/S3 work or implementation change.

## 1. Gate, scope and accepted finding

The independent S1 code/spec review accepted `F-S1-R1-01` (low, documentation correctness): `docs/gateflow/a3-production-security-design-spec-s1-implementation.md` cited accepted-plan commit `0d77f7cebec67335fbc937f0680a07fe9f883950`, but that object does not exist in this repository. Immutable gate evidence must use the real full SHA.

Scope is two literal documentation replacements in the S1 implementation report plus this fix artifact. No validator, model, fixture, canonical design, controller, plan, review artifact, A0/A1/A2 source, prototype, screenshot or other repository path was changed. No historical validation was rewritten and no new design claim was added.

## 2. Exact old/new facts

| Fact | Value |
| --- | --- |
| Stale full SHA (old) | `0d77f7cebec67335fbc937f0680a07fe9f883950` |
| Old object verification | `git cat-file -t 0d77f7cebec67335fbc937f0680a07fe9f883950` → `fatal: git cat-file: could not get object info` (object absent) |
| Correct full SHA (new) | `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c` |
| New object type | `commit` |
| New commit subject | `gateflow: accept plan for A3-production-security-design-spec` |
| New commit paths | exactly five accepted-plan/controller/review paths, below |

The accepted-plan commit contains exactly:

```text
A  docs/gateflow/a3-production-security-design-spec-plan-fix-1.md
A  docs/gateflow/a3-production-security-design-spec-plan.md
M  docs/gateflow/project-execution-control.md
A  docs/reviews/plan-review-20261025-205516.md
A  docs/reviews/plan-review-20261025-212413.md
```

Both stale occurrences were in the S1 implementation report: the `Base` header line (former line 13) and the §1 binding-contract sentence (former line 19). Both now read `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c`. The replacement is 40 characters for 40 characters, so line count and byte count are unchanged.

## 3. Exact changed paths and SHA-256

Only the two paths allowed by the fix task changed.

| Path | Change | Lines | Bytes | SHA-256 |
| --- | --- | ---: | ---: | --- |
| `docs/gateflow/a3-production-security-design-spec-s1-implementation.md` | replaced two stale full-SHA citations | 228 | 23,395 | `8BCB51867B417092F5638402462A0CB5FF518569FE0722BDBFFE2343922A2158` |
| `docs/gateflow/a3-production-security-design-spec-s1-fix-1.md` | this artifact (new) | this artifact | this artifact | reported in the final response; re-verifiable by the controller |

Pre-fix baseline for the implementation report was SHA-256 `DB806F842314E5E011EEB06B5A0C0114F23E6C1797E637563F74BBEB293992F0` at 228 lines / 23,395 bytes (as recorded by correction 2). The changed file keeps the same line and byte counts, confirming the delta is exactly the two 40-character SHA substitutions.

Unchanged fingerprints re-verified after this task: `docs/17-production-security-design-spec.md` SHA-256 `B252B2B96B4AD50C8ACC4AA8D97C6AF2C48C91F0A95245FEB8AEFB1E665A9581`; review artifact SHA-256 `7A54C56A8A09BD3C3C54D42E19142C4C24A5A29C457CE86DCAF0C7EF7983189D`. The security model, validator, six fixtures, accepted plan and controller were not modified and remain out of this task's write scope.

## 4. Validation

Run from `/mnt/d/codex-call`.

| Check | Command | Result |
| --- | --- | --- |
| Corrected SHA resolves | `git cat-file -t 0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c` | `commit` |
| Subject and content | `git show --name-status --format='%H%n%s' <sha>` | expected subject; exactly five plan/review/controller paths |
| Stale SHA absent | `git cat-file -t 0d77f7cebec67335fbc937f0680a07fe9f883950` | `fatal: could not get object info` |
| No stale SHA in report | `grep -n '0d77f7cebec67335fbc937f0680a07fe9f883950' <report>` | no matches |
| Correct SHA present | `grep -n '0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c' <report>` | two matches, lines 13 and 19 |
| No other `0d77f7c…` full forms | prefix grep excluding the corrected SHA | no matches |
| UTF-8 | `file -bi <report>` | `text/plain; charset=utf-8` |
| Conflict markers | line-start grep for `<<<<<<<`, `=======`, `>>>>>>>` | no matches |
| Trailing whitespace | `grep -nE '[[:blank:]]$' <report>` | no matches |
| Diff check | `git diff --check -- <report>` | exit 0, no output |

Both allowed paths are untracked, so plain `git diff --check` has no content to scan; `git diff --no-index --check /dev/null <path>` is used as the meaningful whitespace check for each and produces exit 1 with zero bytes of output (files differ from `/dev/null`, no whitespace diagnostics).

No executable validator/model/fixture run was performed or needed: this fix changes only two citation strings and does not touch any executable or data artifact.

## 5. Documentation decision

- The S1 implementation report was corrected in place because the stale SHA is an immutable-evidence citation error, not a historical validation claim. Its historical sections (§5.2 denial record, §5.3 expected counters, §13/§14 addenda) are untouched.
- No correction addendum was appended to the implementation report; this fix artifact is the sole record of `F-S1-R1-01`, avoiding duplicate or divergent gate history.
- `docs/17-production-security-design-spec.md` was **not** changed even though it carries the same stale SHA at its header (line 9), because it is the canonical design and read-only in this task. It is recorded below as a residual for a separately authorized fix.
- The review artifact and controller are historical/accepted evidence and were not modified.

## 6. Residual risks and uncovered areas

| Residual / uncovered area | Owner / destination |
| --- | --- |
| `R-A3-S1-STALE-SHA-CANONICAL` — `docs/17-production-security-design-spec.md` line 9 still cites the non-existent `0d77f7cebec67335fbc937f0680a07fe9f883950`; the canonical document must use `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c` | Controller; separately authorized canonical-design correction task |
| Historical review text — the review artifact records the stale SHA in its Open Questions as the finding itself; this is intentional historical evidence and must not be rewritten | Controller / review archive |
| Abbreviated SHA — `0d77f7c` appears in review/controller prose as an unambiguous short prefix; full SHA is present in the controller and now in the S1 implementation report | No action; prefix is legitimate |
| No executable revalidation — this documentation-only fix produces no validator/fixture evidence and none is expected | Controller hash-bound documentation re-check |
| `R-A3-HMAC-ROTATION-REPLAY`, `R-A3-KMS-CAPABILITY`, `R-A3-COS-CAPABILITY`, `R-A3-KEY-DESTRUCTION`, `R-A3-LEGAL-RETENTION`, `R-A3-NO-WORM`, `R-A3-REGION-SKU`, static-validator co-authorship | Unchanged from the S1 implementation report §9 and correction artifacts; not affected by this fix |

## 7. Completion status and artifact path

- Status：**complete** for this bounded fix. `F-S1-R1-01` is author-complete: the two stale full-SHA citations in the S1 implementation report now use the real accepted-plan commit `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c`, verified by object type, subject and five-path content; no stale full SHA remains in the report.
- Changed paths：`docs/gateflow/a3-production-security-design-spec-s1-implementation.md` (SHA-256 `8BCB51867B417092F5638402462A0CB5FF518569FE0722BDBFFE2343922A2158`), `docs/gateflow/a3-production-security-design-spec-s1-fix-1.md` (this artifact).
- Artifact path：`docs/gateflow/a3-production-security-design-spec-s1-fix-1.md`.
- No self-review, code review, staging, commit, push, PR, merge, branch change, controller update, S2 or S3 work, or implementation change was performed. This task stops here.
