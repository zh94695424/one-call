# PA0 Boot 4.1 plan fix 1 — F-PA0-BR-01 residual range

- Task label: `pa0-planfix-boot41-ds-20261027-01`
- Work unit: `PA0-production-runtime-bootstrap`
- Gate: `plan fix` (only the controller-accepted finding below; no implementation, no other gate)
- Date: 2026-10-27 (Asia/Shanghai)
- Review source: `docs/reviews/plan-review-20261027-042142.md` §5, `F-PA0-BR-01` (Low, `未修复`)
- Plan target before fix: SHA-256 `6131FC6C28DB2C083138B23A3EB650DBF81C4BCEA193E565FADE51173BF742D1`, 1364 lines
- Plan target after fix: SHA-256 `415f1f2e658623059f8a2d63156f05f10685eae702cf27077f695ef7bb6d3456`, 1370 lines

## 1. Finding

`F-PA0-BR-01` (Low): §13.6 completion-report template told S1/S2 authors to reconcile residuals `PA0-R-01..13`, while §13.3 had frozen the new governance residual `PA0-R-14` (Modulith `2.1.x` `ACTIVE_UNSUPPORTED` / `support=none`). An otherwise honest S1/S2 report could omit R-14's owner/destination line, silently dropping Modulith `support=none` tracking from the residual ledger even though §13.3 requires controller acceptance and 2.1.x patch tracking.

## 2. Exact old/new wording

File: `docs/gateflow/pa0-production-runtime-bootstrap-plan.md`, §13.6 template.

Old (line 1303):

```text
residual owner/destination reconciliation (PA0-R-01..13)
```

New (lines 1303–1309):

```text
residual owner/destination reconciliation (PA0-R-01..14):
  each ID PA0-R-01 through PA0-R-14 -> owner -> destination, none omitted
  PA0-R-14: Modulith 2.1.x ACTIVE_UNSUPPORTED/support=none ->
    owner Platform/Security + release governance ->
    destination controller acceptance at revised plan review;
    S1/S2 record Modulith 2.1.2 resolved version and track 2.1.x patches,
    no silent substitution
```

## 3. Why this bounded change closes the finding

- The template range is now exactly `PA0-R-01..14`, matching §13.3's table which ends at `PA0-R-14`; no "all risks" shortcut.
- It explicitly requires each ID's owner and destination, "none omitted", so R-14 cannot be skipped.
- It names R-14's owner (`Platform/Security + release governance`) and destination (controller acceptance at revised plan review; S1/S2 record Modulith `2.1.2` resolved version, track 2.1.x patches, no silent substitution), matching §13.3/§13.4 VQ-05 without reclassifying the residual.
- No other template field, instruction, scope, pin, slice, support decision or validation requirement changed.

## 4. Validation

- §13.3/§13.6 re-read: §13.3 residual table ends at `PA0-R-14` (`support=none` classification preserved verbatim); §13.6 now states exactly `PA0-R-01..14` and names R-14's owner/destination.
- `grep -n "PA0-R-01\.\.1[34]"` → exactly one match, line 1303 `PA0-R-01..14`; zero remaining `PA0-R-01..13` occurrences in the plan.
- `git diff --check -- docs/gateflow/pa0-production-runtime-bootstrap-plan.md` → exit 0 (no whitespace errors).
- Diff hunk (`git diff -U1`): only the residual line region changes (`-residual ... (PA0-R-01..13)` / `+` 7 lines); line count 1364 → 1370 (single 1-line replacement expanded to 7 lines, +6).
- Active target unchanged: §4 pins remain `Spring Boot 4.1.2` / `Spring Modulith 2.1.2`; all `4.0.x` mentions remain forbidden-fallback/stop-condition contexts (lines 11, 19, 125, 204, 234, 236, 1056, 1083, 1188, 1217, 1229, 1245, 1269, 1368); no Boot 3.x/Modulith 1.x/2.0.x active pin introduced.
- No other plan lines changed beyond this finding; all other working-tree diff hunks are the pre-existing Boot 4.1 revision content that the review already measured (pre-fix hash above).

## 5. Changed / unchanged paths

Changed (allowed only):
- `docs/gateflow/pa0-production-runtime-bootstrap-plan.md`
- `docs/gateflow/pa0-production-runtime-bootstrap-plan-boot41-fix-1.md` (this artifact, new)

Unchanged (forbidden proof):
- `git status --porcelain` for `docs/gateflow/project-execution-control.md`, `docs/reviews/**`, `docs/gateflow/pa0-production-runtime-bootstrap-plan-boot41-revision.md` shows the same pre-existing states as the session-start snapshot (` M` for control/review files, `??` for the boot41 revision and `plan-review-20261027-042142.md`); no new add/delete/rename.
- mtimes confirm no touch by this task: plan edit at `2026-10-27 04:27:11 +0800`; review artifact `04:23:37`, boot41 revision `04:14:38`, control doc `04:03:17` (all earlier).
- No `backend/**`, `.github/**`, prototypes/screenshots, `.codex-tasks/**` or other files modified; no staging, commit, push, PR, merge, branch switch, code implementation, code review or plan re-review performed.

## 6. Residual risks (unchanged)

- `PA0-R-14` classification remains `ACTIVE_UNSUPPORTED` / `support=none`, owner `Platform/Security + release governance`, destination controller acceptance at revised plan review plus S1/S2 tracking of Modulith `2.1.2` / 2.1.x patches.
- All other §13.3 residuals (`PA0-R-01..13`) and review residual risks `RSK-BR-01..04` are untouched.
- Fix remains docs-only and does not reduce any preflight/evidence/validation requirement; S1/S2 still must reconcile every residual explicitly.

## 7. Completion status

**COMPLETE** — `F-PA0-BR-01` fixed by the bounded §13.6 edit above. Awaiting controller revalidation/acceptance; no commit, push, PR or next gate entered.
