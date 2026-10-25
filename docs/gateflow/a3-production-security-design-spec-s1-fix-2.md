# A3 S1 bounded code-review fix 2 — canonical header full SHA correction

> Work unit：`A3-production-security-design-spec`
>
> Slice：S1 — sensitive-data protection, key hierarchy, rotation/recovery and historical replay
>
> Gate：`fix` (documentation correctness only; not implementation expansion or re-review)
>
> Task label：`a3-security-s1-reviewfix2-ds-20261025-01`
>
> Provider/model：Claude Code / `ds` / `deepseek-flash[1m]`
>
> Binding review artifact：`docs/reviews/code-review-20261025-222947.md`, SHA-256 `7A54C56A8A09BD3C3C54D42E19142C4C24A5A29C457CE86DCAF0C7EF7983189D` (read-only, unchanged)
>
> Stop condition：this bounded documentation fix only. No self-review, code review, staging, commit, push, PR, merge, branch change, controller update, S2/S3 work or implementation change.

## 1. Gate, scope and accepted finding

The independent S1 code/spec review accepted `F-S1-R1-01` (low, documentation correctness). Fix 1 corrected the two stale accepted-plan SHA citations in `docs/gateflow/a3-production-security-design-spec-s1-implementation.md` and correctly disclosed the remaining canonical citation: `docs/17-production-security-design-spec.md` line 9 still cited absent object `0d77f7cebec67335fbc937f0680a07fe9f883950`. This fix completes that residual.

Scope is one literal documentation replacement in the canonical S1 design header plus this fix artifact. No validator, model, fixture, implementation report, fix-1/review artifact, controller, plan, A0/A1/A2 source, prototype, screenshot or other repository path was changed. No historical validation was rewritten and no new design claim was added.

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

The single stale occurrence was the `Base` header line (line 9) of `docs/17-production-security-design-spec.md`. It now reads `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c`. The replacement is 40 characters for 40 characters, so line count and byte count are unchanged (297 lines / 33,790 bytes).

## 3. Exact changed paths and SHA-256

Only the two paths allowed by the fix task changed.

| Path | Change | Lines | Bytes | SHA-256 |
| --- | --- | ---: | ---: | --- |
| `docs/17-production-security-design-spec.md` | replaced one stale full-SHA citation | 297 | 33,790 | `5B0BDA88416DF0C23362886451D744277DAA81A96DE846675B16E5C81F448C61` |
| `docs/gateflow/a3-production-security-design-spec-s1-fix-2.md` | this artifact (new) | this artifact | this artifact | reported in the final response; re-verifiable by the controller |

Pre-fix fingerprint of the canonical design was SHA-256 `B252B2B96B4AD50C8ACC4AA8D97C6AF2C48C91F0A95245FEB8AEFB1E665A9581` at 297 lines / 33,790 bytes (as recorded by fix 1). The changed file keeps the same line and byte counts, confirming the delta is exactly the one 40-character SHA substitution.

Unchanged fingerprints re-verified before and after this task (identical):

| Artifact | SHA-256 |
| --- | --- |
| `security/validate.mjs` | `BB5BF3B5B797B2A16193AFDE23B973D6B86992D24DF5A46EB09D71BC95D6E025` |
| `security/v1/security-design-v1.json` | `13E08CC6A01BEE69F2ADC6C57842B5A5D1B0D9147FAE6771E84E6EE1301AB81D` |
| `security/v1/fixtures/sec-s1-01-envelope-purpose-roundtrip.json` | `844B67A4F10A8DCBCD690DF6093E9DE33DC8BC964AC210A4850E89D1F806F576` |
| `security/v1/fixtures/sec-s1-02-hmac-index-equality.json` | `2ACEC51FD57E468AF58546F35A9DD1021DE2CBEDAD75C5610F3963E45155371D` |
| `security/v1/fixtures/sec-s1-03-cross-purpose-denied.json` | `5CBFA710DC2A88B6A44F87590B3BA580370E51B5E528DB4F349F1F88282142C5` |
| `security/v1/fixtures/sec-s1-04-rotation-historical-replay.json` | `6A9FBD95D4971A2B551BCDA28CA209FC173A9E3A9CA1D4C00841B7ACB3737206` |
| `security/v1/fixtures/sec-s1-05-key-unavailable-failclosed.json` | `CAA71DFFC8C2D015130BF01F0CE95AE69DAE7BA963A819275DC157A5EB107C7D` |
| `security/v1/fixtures/sec-s1-06-append-only-rotation-safety.json` | `00D85E4401E6E31F858804AB600A6E3D1E694A4E1C2F938F00588DA6C4B2C0E2` |
| `docs/reviews/code-review-20261025-222947.md` | `7A54C56A8A09BD3C3C54D42E19142C4C24A5A29C457CE86DCAF0C7EF7983189D` |

The accepted plan and controller were not modified and remain out of this task's write scope.

## 4. Validation

Run from `/mnt/d/codex-call`.

| Check | Command | Result |
| --- | --- | --- |
| Corrected SHA resolves | `git cat-file -t 0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c` | `commit` |
| Subject and content | `git show --name-status --format='%H%n%s' <sha>` | expected subject; exactly five plan/review/controller paths |
| Stale SHA absent | `git cat-file -t 0d77f7cebec67335fbc937f0680a07fe9f883950` | `fatal: could not get object info` |
| No stale SHA in live S1 source | `grep -rn '0d77f7cebec67335fbc937f0680a07fe9f883950' docs/17-production-security-design-spec.md security/ docs/gateflow/a3-production-security-design-spec-s1-implementation.md` | no matches |
| Correct SHA present once in canonical design | `grep -c '0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c' docs/17-production-security-design-spec.md` | `1` (line 9) |
| S1 stage validator run 1 | exact §7.1 command, output captured | exit 0 |
| S1 stage validator run 2 | same command, output captured | exit 0 |
| Determinism | `sha256sum /tmp/s1-run1.txt /tmp/s1-run2.txt`; `cmp` | both `3E991F1D7A075E826E70E7C2654568019A6A3644383ECECE4699AD82B2CAB0C6`; identical |
| S1 counters | run output | `key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=6 fixtures=6 passed=6 failed=0 negative=2`; `drift=0 doc_missing=doc_extra=doc_duplicate=doc_invalid=token_missing=0`; `stage_field_missing=stage_field_premature=fixture_id_missing=fixture_id_extra=0`; `PASS security-validation (s1)` |
| Validator syntax | `node --check security/validate.mjs` | exit 0 |
| A1 contract | `cd api/openapi && npm run check:s3` | exit 0; `PASS check-doc-map (s3)`, operations=49, expected_counts=18/21/6/3/1 |
| A2 protocol | accepted A2 stage-S3 validator (§7.1 command) | exit 0; `PASS protocol-validation (s3)`, fixtures=25 passed=25 failed=0 |
| Model/validator/fixtures unchanged | before/after `sha256sum` | all identical, table in §3 |
| UTF-8 | `file -bi docs/17-production-security-design-spec.md` | `text/plain; charset=utf-8` |
| Conflict markers | line-start grep for `<<<<<<<`, `=======`, `>>>>>>>` | no matches |
| Trailing whitespace | `grep -nE '[[:blank:]]$' docs/17-production-security-design-spec.md` | no matches |
| Diff check | `git diff --check -- docs/17-production-security-design-spec.md` | exit 0, no output |

`docs/17-production-security-design-spec.md` is untracked in this work unit, so plain `git diff --check` has no content to scan; `git diff --no-index --check /dev/null docs/17-production-security-design-spec.md` is used as the meaningful whitespace check and produces exit 1 with zero bytes of output (file differs from `/dev/null`, no whitespace diagnostics).

## 5. Documentation decision

- `docs/17-production-security-design-spec.md` was corrected in place because the stale SHA is an immutable-evidence citation error in the canonical header, not a historical validation claim. The rest of the document, including all S1 semantic sections, is untouched.
- No correction addendum was appended to the canonical design or the S1 implementation report; fix 1 and this fix artifact together are the sole record of `F-S1-R1-01`, avoiding duplicate or divergent gate history.
- Historical references that describe the defect were intentionally left unchanged: `docs/gateflow/a3-production-security-design-spec-s1-fix-1.md`, `docs/reviews/code-review-20261025-222947.md`, `docs/gateflow/project-execution-control.md` (line 152) and the task brief. Rewriting them would falsify the audit trail.
- The review artifact and controller are historical/accepted evidence and were not modified.

## 6. Residual risks and uncovered areas

| Residual / uncovered area | Owner / destination |
| --- | --- |
| Historical stale-SHA text — review, fix-1 and controller still contain `0d77f7cebec67335fbc937f0680a07fe9f883950` as the finding/defect record; this is intentional immutable evidence | Controller / review archive; no action |
| Abbreviated SHA — `0d77f7c` appears in review/controller prose as an unambiguous short prefix; full SHA now appears in both the controller and the canonical design header | No action; prefix is legitimate |
| Textual gate evidence only — this documentation-only fix adds no new executable claim; the validator/fixture runs above re-assert the unchanged frozen state | Controller hash-bound documentation re-check |
| `R-A3-HMAC-ROTATION-REPLAY`, `R-A3-KMS-CAPABILITY`, `R-A3-COS-CAPABILITY`, `R-A3-KEY-DESTRUCTION`, `R-A3-LEGAL-RETENTION`, `R-A3-NO-WORM`, `R-A3-REGION-SKU`, static-validator co-authorship | Unchanged from the S1 implementation report §9 and correction artifacts; not affected by this fix |

## 7. Completion status and artifact path

- Status：**complete** for this bounded fix. `F-S1-R1-01` is author-complete: the canonical design header now cites the real accepted-plan commit `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c`, verified by object type, subject and five-path content; no stale full SHA remains in live S1 source, and the correct SHA appears exactly once in `docs/17-production-security-design-spec.md`.
- Changed paths：`docs/17-production-security-design-spec.md` (SHA-256 `5B0BDA88416DF0C23362886451D744277DAA81A96DE846675B16E5C81F448C61`), `docs/gateflow/a3-production-security-design-spec-s1-fix-2.md` (this artifact).
- Artifact path：`docs/gateflow/a3-production-security-design-spec-s1-fix-2.md`.
- No self-review, code review, staging, commit, push, PR, merge, branch change, controller update, S2 or S3 work, or implementation change was performed. This task stops here.
