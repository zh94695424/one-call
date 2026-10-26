# A5 Draft PR #7 bounded review fix 1 (STATUS: COMPLETE, dynamic validation blocked)

- Work unit: `A5-production-acceptance-matrix-spec`
- Gate: `PR review fix` 1
- Task label: `a5-production-acceptance-matrix-pr7-fix1-ds-20261026-01`
- Provider/model: deepseek-flash[1m]
- Date: 2026-10-26
- PR review: `docs/reviews/pr-7-review-20261026-203107.md`
- Accepted PR head before fix: `cc8e28fa53a551f9a77b30fbc0fd117db0becd54`
- This artifact: `docs/gateflow/a5-production-acceptance-matrix-spec-pr7-fix-1.md`

## Accepted findings

### F-PR7-01 (Low) — `A5_COUNT_ONLY_COVERAGE` declared but never emitted

The binding plan registered a count-only fail-safe and the reason code exists in
`REASON_CODES` and the matrix `reasonRegistry`, but no validator path emitted it. Coverage
completeness was still derived from the full gate AND; only the named guard was missing.

### F-PR7-02 (Low) — `A5_DETERMINISM_FAILURE` declared but never emitted; `deterministic=true` hardcoded

`summarize()` unconditionally printed `deterministic=true`. The reason code existed but no
double-run comparison or emission path existed, so the tool asserted a property it did not
verify.

## Design decisions

### F-PR7-01 — exact caller aggregate-coverage guard

- `validateRoot()` now treats the two recognized model-root fields
  `specificationCoverageComplete` and `specCoverageComplete` as forbidden caller/model
  aggregate-coverage assertions and emits exactly `A5_COUNT_ONLY_COVERAGE` for each present
  field (`model.<field>` path, field name as id).
- Both names are excluded from the generic `A5_SECTION_EXTRA` loop, so a focused mutation yields
  exactly `A5_COUNT_ONLY_COVERAGE` and no companion section-extra finding.
- No speculative field names and no generic heuristic were added. The guard only rejects this
  root shortcut; it does not replace any root/ID/ref/owner/coverage/semantic/doc/baseline check.
- Two focused exact-code cases were added to `s1-01-matrix-integrity.json` (25 -> 27 cases). The
  existing `all-counts-but-unbacked-na` (S1) and `count-only-acceptance` (S2) cases remain and
  still prove counts cannot bypass semantic/evidence gates.

### F-PR7-02 — CLI-boundary two-probe determinism gate

- `--determinism-probe` is a parser-recognized internal flag. Its only routing effect is to
  prevent recursive parent spawning: a normal invocation runs the gate, a probe invocation runs
  the complete validation path directly.
- The parent resolves the current entry path (`process.argv[1]`), then launches two sequential
  isolated child processes with `process.execPath` and the exact original arguments plus the
  probe flag, preserving `cwd`. It reads no environment values, wall clock, network or
  randomness. Output capture is bounded at 64 MiB per stream.
- Each probe executes the full validation path (all inputs, A0-A4 context, fixtures, evidence and
  `--check-doc-links` mode). `summarize()` emits the non-final marker `deterministic=probe`;
  a validator process never emits `deterministic=true` itself.
- The parent compares exit status, stdout and stderr byte-for-byte and explicitly rejects
  `spawnSync.error`, null/signalled status and non-string outputs.
- Identical normal failure: relay the first failure's stdout/stderr and exit status unchanged.
  Identical success: require exactly one `deterministic=probe` marker, replace only that marker
  with `deterministic=true`, relay once and preserve the child exit status. Success stdout is
  therefore byte-identical to the accepted S1/S2 canonical output.
- Launch failure, unusable signal/status, output mismatch or a success output without exactly one
  probe marker fail closed via `A5_DETERMINISM_FAILURE` with stable path `determinism.probe` and
  id `two-probe`, exit 1 and no PASS line.
- This checks only same-input validator output determinism. It does not check external runtime
  systems, DB/Redis/COS/KMS/line/cloud behavior, wall-clock behavior or production readiness.

## Exact changed files (five allowed paths)

1. `acceptance/validate.mjs` (modified: root count-only guard; `--determinism-probe` parser flag;
   `main` -> two-probe gate; `runValidation` split; probe spawn/compare; probe marker in summary)
2. `acceptance/fixtures/s1-01-matrix-integrity.json` (modified: +2 focused exact-code cases)
3. `docs/19-production-acceptance-matrix.md` (modified: caller aggregate-coverage guard note;
   determinism validation notes)
4. `docs/gateflow/a5-production-acceptance-matrix-spec-plan.md` (modified: §8.3 items 10/12,
   §9.1 fixture table, §10.2/§10.4 validation assertions)
5. `docs/gateflow/a5-production-acceptance-matrix-spec-pr7-fix-1.md` (created: this artifact)

No matrix JSON, reason registry, scenario matrix content, S1/S2 implementation report, review,
control bookkeeping, prototype, screenshot or `.codex-tasks/` path was changed.

Working-tree SHA-256 for the four modified paths (this artifact's own digest is controller-owned):

```text
5556331c9f6090858faeec1ff22da1bc5c27e7f37872124c379108d583fa74bc  acceptance/validate.mjs
80c8b610495c9987f20ae0c5bcfb65d7ca82da118f4c791197471b443a1a5b8e  acceptance/fixtures/s1-01-matrix-integrity.json
2da6fc4ce26852db0786514a0439ac7132f8824942fd2b87b5f62f8e7a33d3f5  docs/19-production-acceptance-matrix.md
f96e6d3063a2433c018488bd3eade4fe70e3e178b6070ec61d45b635d03bf9f0  docs/gateflow/a5-production-acceptance-matrix-spec-plan.md
```

## Call paths

- Root count-only guard:
  `main` -> `parseArgs` -> `runDeterminismGate` -> `spawnProbe` -> child `main` ->
  `runValidation` -> `evaluate` -> `validateRoot` -> `root(A5_COUNT_ONLY_COVERAGE)`.
- Fixtures: `runFixtures` -> `evaluate` -> `validateRoot` for the two new `setPath` mutations.
- Determinism parent: `main` -> `parseArgs` -> `runDeterminismGate` -> `spawnProbe` x2 ->
  compare status/stdout/stderr -> relay or `failDeterminism`.
- Determinism probe: child `main` -> `parseArgs` (probe flag present) -> `runValidation` ->
  `summarize(..., deterministic=PROBE_MARKER)`.

## Validation results

Dynamic execution was permission-blocked in this session. Every `node` file execution
(`node --check`, validator runs, A1/A2/A3/A4 stages), and also `python3 -m json.tool` and `jq`,
returned `This command requires approval`; `node --version` was available (`v22.23.4`) but no
validator command started. No exit code or behavioral pass is claimed for those checks. The
controller will rerun the complete suite.

| Check | Result |
| --- | --- |
| `node --check acceptance/validate.mjs` | BLOCKED (permission gate), no exit code |
| A5 S1 exact command twice; hash `085752ae…1280` | BLOCKED, not re-run; by construction normal stdout remains byte-identical (probe marker is replaced by `deterministic=true`) but this is not claimed as executed evidence |
| A5 S2 exact command twice + `--check-doc-links`; hash `810029be…ba9` | BLOCKED, not re-run; same construction argument, not claimed as executed evidence |
| Direct internal-probe S1 and S2 | BLOCKED; static trace shows `summarize` emits exactly one `deterministic=probe` and no process emits `deterministic=true` |
| Focused mutations for both root spellings | BLOCKED dynamically; fixture cases added; static trace shows only `A5_COUNT_ONLY_COVERAGE` because both names are excluded from `A5_SECTION_EXTRA` |
| All S1/S2 fixtures exact primary-code sets | BLOCKED; existing cases untouched, two new cases use the same `setPath` mutation shape as `caller-production-accepted` |
| A1 `npm run check:s3`, A2 stage s3, A3 stage s3, A4 stage s2 | BLOCKED (A1-A4 inputs untouched) |
| Five A5 JSON parses | BLOCKED for node/python/jq; fixture edit is a balanced additive two-object insertion; matrix unchanged |
| `git diff --check` on the five A5 paths | PASS, exit 0, no output |
| Docs/19 relative links and README uniqueness | PASS statically: one relative-link line, both targets exist; exactly one docs/19 row in `docs/README.md:51` |
| Task scope | Exactly the five allowed paths (git status/diff stat); all other dirty paths are pre-existing controller bookkeeping and untouched |
| Fixture integrity | 27 caseIds (25 -> 27); SHA-256 `80c8b610495c9987f20ae0c5bcfb65d7ca82da118f4c791197471b443a1a5b8e` |

The whole-worktree `git diff --check` exits 2 on pre-existing CRLF/trailing-whitespace changes in
`deployment/capacity/calc.mjs`; that path is outside this task and was not modified here. The
scoped check on the five allowed paths is clean.

## Docs decision

The dedicated reason is now documented as the caller aggregate-coverage guard, not as a proof of
coverage completeness. `docs/19` §8 and §10 and plan §8.3/§9.1/§10 describe the exact field names,
the no-section-extra behavior, the two-probe parent/probe contract and the dedicated failure
reason. The docs explicitly keep the unbacked-N/A and S2 count-only acceptance cases as the proof
that counts cannot bypass underlying semantic/evidence gates. `docs/README.md`, historical reviews
and S1/S2 implementation reports were not modified.

## Residual risks and uncovered areas (classified, with owner/destination)

- `LOW` — Dynamic validation (node syntax/S1/S2/direct probes/fixtures/A1-A4/JSON parses) was
  blocked by this session's permission gate. Owner: controller. Destination: controller full-suite
  rerun before acceptance.
- `LOW` — The immutable S1 implementation report records `s1-01` at 25 cases and its old SHA-256;
  the accepted fix intentionally changes the fixture to 27 cases and its digest. The report is
  historical evidence and was not rewritten. Owner: controller/closeout. Destination: A5 PR fix
  acceptance / closeout bookkeeping.
- `LOW` — The two-probe gate adds a cold start and two child runs per top-level invocation; this
  is bounded runtime cost, not a correctness risk. Owner: Platform/CI. Destination: repository CI
  policy before production merge.
- `NOT_RUN` — Runtime, DB, Redis, COS/KMS, payment, IVR line/B-leg, cloud, restore, load and
  manual acceptance were not executed and are not claimed. Owner: production acceptance.
  Destination: future runtime acceptance gates (`productionAccepted=false`, `runtimeEvidence=NOT_RUN`).
- `NOT_RUN` — The determinism gate proves only same-input validator output determinism; it does
  not prove external runtime determinism. Owner: production acceptance. Destination: future
  runtime acceptance gates.

## Completion status

`COMPLETE` — both accepted Low findings are implemented within the five-path bound:
`A5_COUNT_ONLY_COVERAGE` now has an exact caller aggregate-coverage emission path with focused
fixtures, and `deterministic=true` is now only published after a two-probe CLI-boundary comparison.
Dynamic validation was permission-blocked and is reported honestly; no PASS result is claimed for
blocked commands. No runtime, carrier, IVR-line, cloud, database or production acceptance is
asserted.

- Artifact path: `docs/gateflow/a5-production-acceptance-matrix-spec-pr7-fix-1.md`
- Stop condition: no staging, commit, push, PR/issue metadata action, merge, re-review entry, gate
  change or further fix was performed.
