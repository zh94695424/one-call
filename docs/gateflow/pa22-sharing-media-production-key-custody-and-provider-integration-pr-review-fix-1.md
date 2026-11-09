# PA22 PR-review fix 1 — head audit traceability

- Task: `pa22-pr33-fix1-gpt-20261110-01`; date: `2026-11-10`.
- Work unit: `PA22-sharing-media-production-key-custody-and-provider-integration`.
- Runtime/provider: Codex runner / GPT `gpt-6.1-sol` / high.
- Scope: only this new fix artifact and `docs/gateflow/project-execution-control.md`.

## Accepted finding and evidence

- Reviewer A: `docs/reviews/pr-33-review-20261110-041529.md`, 153 lines / 20,106 bytes / SHA256 `46F2FBD505059511691770953F128B4C603C5A1C9EF06F4E7C2FAF4FC80F4D78`; `pass-with-risks`, exactly one Low finding accepted by the Controller. Control's claim that the remote branch and `refs/pull/33/head` still resolved to the initial `2c52d6e1` was stale after the bookkeeping push. This is audit traceability only; no product defect was found.
- Reviewer B: `docs/reviews/pr-33-review-20261110-042908.md`, 60 lines / 10,360 bytes / SHA256 `9E5265F5A7CD9603E8EBDBDA9FBEDEAAFB7F658F14260D111755B104B8F45EE6`; `pass`. Its open question about the same stale head is resolved by this correction.
- Both independent review artifacts were submitted before the Controller read either. Read-only inputs are `/mnt/c/Users/Administrator/AppData/Local/Temp/pa22-pr-fix1-08111592/review-a.md` and `/mnt/c/Users/Administrator/AppData/Local/Temp/pa22-pr-fix1-08111592/review-b.md`; their measured line counts, byte counts and SHA-256 values match the records above. No existing review artifact is imported or modified by this fix.

## Exact Control correction

- Preserve `2c52d6e1e83448e6b853a0b7e3c2f55cdd69cfcc` as the initial successful push / Draft-PR / readiness head and preserve the Draft PR #33 creation record.
- Replace the stale present-tense remote/ref assertion with that historical qualification. Explicitly record that subsequent `gateflow: start PA22 PR review` commit `e4c986c47909fcf8981d925346a04f53f57b37ed` advanced the branch and, after its push, the remote branch and `refs/pull/33/head`; `e4c986c4` is the frozen PR-review head.
- Add A/B verdicts, artifact identities, Controller adjudication and head-specific CI evidence. Set the latest current gate / next entry point to `re-review`, without accepting PR review or rewriting unrelated history.
- Controller-supplied observations on `e4c986c47909fcf8981d925346a04f53f57b37ed`: `contract-regressions=SUCCESS`, `java-runtime=SUCCESS`, PR #33 Draft / MERGEABLE / CLEAN. These are not fresh remote observations by this task and do not establish CI success for any later head or this uncommitted fix.

## Validation and unchanged boundaries

- Local history confirms `e4c986c4` is `gateflow: start PA22 PR review`, immediately following `2c52d6e1` (`gateflow: prepare PA22 draft PR`). The Windows-style `.git` pointer is left untouched; read-only Git commands use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call24` and an explicit `GIT_WORK_TREE` for this workspace.
- `git diff --check` passes. Final status contains exactly the modified Control, this new fix artifact and the pre-existing untracked `.codex`; `.codex` is untouched. Final file line/byte counts and SHA-256 values are reported at handoff rather than embedding a self-referential artifact hash.
- No product/test change or test execution: source, tests, CI, POM, OpenAPI, database, deployment, plan and existing review artifacts remain unchanged. No commit, push, PR mutation, approval, mark-ready action, reviewer request, external comment or issue mutation is performed.

## Residual risk and next gate

- Residual risk: Low, documentation-only; later pushes can make head-specific observations stale again. Controller owns recording any later head and obtaining its own CI evidence. Existing owner-routed product/operational residuals remain unchanged; no new product assurance is inferred from this fix.
- Finding disposition: corrected, pending independent PR re-review. Next gate: `re-review`; PR review is not accepted and no later gate is authorized here.
