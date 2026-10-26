# A4 S2 implementation review — a4-production-capacity-deployment-spec (MiMo 2.6 Flash)

> Gate: S2 implementation code review (independent, read-only)
>
> Task label: `a4-capacity-s2-review-mimo26flash-20261026-01`
>
> Reviewer model: MiMo 2.6 Flash (`mimo-v2.6-flash`), independent reviewer
>
> Branch: `codex/a4-production-capacity-deployment-spec`
>
> Controller-declared base: `04c63de0e3243100370325f36e13de8a0791c2f4` — **this object does not exist in the repository** (`git diff` → `fatal: bad object`). The actual accepted S1 head is `04c63de87996d14d0d3701b399edf46e2a921ed9` (short `04c63de` matches; the full 40-hex hashes diverge after the 7-char prefix). All deltas below were computed against the real HEAD `04c63de87996d14d0d3701b399edf46e2a921ed9`; see finding F-S2-R1-04.
>
> Compared against: `docs/gateflow/a4-production-capacity-deployment-spec-plan.md` (plan fix 2, accepted), `docs/12-technology-stack-and-system-architecture.md` §4/§5 (read-only upstream), `docs/18-production-capacity-and-deployment-spec.md` (S1+S2 spec)
>
> Reviewed paths (S2 slice): `deployment/capacity/calc.mjs`, `deployment/capacity/validate.mjs`, `deployment/capacity/inputs.template.json`, `deployment/capacity/fixtures/s2-01-backup-recovery.json`, `deployment/capacity/fixtures/s2-02-capability-assertion.json`, `deployment/capacity/fixtures/s2-03-deployment-gates.json`, `docs/18-production-capacity-and-deployment-spec.md` (§7–§10 + scope-header edits), `docs/README.md`, `docs/gateflow/a4-production-capacity-deployment-spec-s2-implementation.md`
>
> Excluded from attribution: the working tree carries large pre-existing modifications to A0–A3 docs/contracts, `protocol/`, `security/`, prototypes, screenshots and `shared/` that predate this slice (visible in the session-start `git status` snapshot and unchanged by the S2 file list). The S2 implementation report §2 correctly lists only its allowed paths; those other dirty paths are controller bookkeeping, not S2 scope.
>
> Status: independent review complete. **Fail** (one P1 fail-open on the six-dependency closure; required fixes listed below). Findings are recommendations for controller adjudication; this artifact does not modify any implementation, fixture, spec, README, control doc, prototype or screenshot, and does not create a checkpoint.

## 1. Evidence separation — reviewer permission limits vs controller evidence

The reviewer's session reproduces the implementer's environment constraint:

| Command | Reviewer result |
| --- | --- |
| `node --version` | exit 0, `v22.23.4` |
| `node --check deployment/capacity/calc.mjs` | **not run** — `This command requires approval` (process never started, no exit code) |
| `node --check deployment/capacity/validate.mjs` | **not run** — same permission block |
| `node deployment/capacity/validate.mjs --stage s1` | **not run** — `This command requires approval` |
| `node deployment/capacity/validate.mjs --stage s2` | **not run** — same |
| `jq`/`file`/`git` static inspection | permitted; used throughout this review |

The reviewer therefore **does not claim** to have executed the syntax checks or validators. The following results are **controller-supplied evidence**, recorded here verbatim as external evidence and not independently re-run by the reviewer:

- `node --check deployment/capacity/calc.mjs` and `node --check deployment/capacity/validate.mjs` — both exit 0.
- `--stage s1` — passed 86/86 fixture cases, 459 non-finite probes.
- `--stage s2` — passed 108/108 fixture cases (86 S1 regression + 22 S2), 471 non-finite probes, 3 repeat-determinism checks, `readmeLink=true`.

**Independent static corroboration of those counts (reviewer-run, no node):**

- Case counts by case-level `"behavior":` occurrences: S1 = 4+6+7+8+11+17+5+17+11 = **86**; S2 = 11 (s2-01) + 7 (s2-02) + 4 (s2-03) = **22**; total **108**. Matches the controller's 86/86 and 108/108.
- `repeat: true` markers: exactly **3** (`s2-01-backup-recovery.json:9` complete-zero-rpo-rto, `s2-02-capability-assertion.json:39` claimed-complete-is-still-not-verified, `s2-03-deployment-gates.json:23` arithmetic-complete-but-not-production-ready). Matches controller `repeatChecks=3`.
- Probe-leaf arithmetic: stage s2 adds `asOf` (unit `iso8601`, excluded from numeric probes by `validate.mjs:17` `NON_NUMERIC_UNITS`), one `probeBackupDependency()` with four numeric `seconds` leaves (`approvedRpoSeconds`/`approvedRtoSeconds`/`observedRpoSeconds`/`observedRtoSeconds` — `validate.mjs:379-391`), and one `probeCapability()` with only `verifiedAt` (`iso8601`, excluded). New probed leaves = 4; × 3 bad values (`NaN`/`+Infinity`/`-Infinity`, `validate.mjs:424-428`) = **+12 probes**. 459 + 12 = **471**. Matches the controller exactly, confirming the S2 probe injection shape (`validate.mjs:415-419`).
- README: the exact plan line exists once at `docs/README.md:50`; a Grep for the link token `18-production-capacity-and-deployment-spec.md` returns exactly that one line. Matches `readmeLink=true`.

Any failure of the controller-supplied runtime results would override the static corroboration above; the reviewer treats them as plausible-but-external.

## 2. Findings — severity-ordered (P0–P3)

### F-S2-R1-01 (P1, blocking) — six-dependency closure is not enforced; a proper subset can emit `BACKUP_CLOSURE_COMPLETE`

**Requirement.** Plan §4.1 (`a4-…-plan.md:117`): "`BACKUP_DEPENDENCY[]`: 固定六个 `dependencyId`：`postgresPitr`、`cosVersions`、`kmsKeyVersions`、`secretVersions`、`reconciliationOutbox`、`ivrLocalFacts`". Plan F-BACKUP-01 (`:294`): "六依赖逐项 typed 完整性 + 聚合 … 全 excluded/缺失 => `copiesOnly:true` + `COPIES_ONLY_INCOMPLETE`". `docs/18:379-381`: "backupRecovery 组必需 `BACKUP_DEPENDENCY[]`，固定六项 `dependencyId`". The S2 task's headline fail-closed priority is the **six dependency closure**.

**Implementation.** `computeBackupRecovery` (`deployment/capacity/calc.mjs:3984-4085`) iterates only the entries the caller supplied (`entries.forEach`, `:4006-4010`). There is **no check** that all six IDs in `BACKUP_DEPENDENCY_IDS` (`calc.mjs:138-145`) appear in the array, and no per-entry presence marker for absent IDs. The closure verdict is then computed as:

- `copiesOnly = entriesEmpty || states.every(s => !s.usablyIncluded)` (`:4012-4013`) — a single structurally-complete entry makes `copiesOnly` false;
- `closureStatus` falls through to `worst(metrics.map(m => m.status))` (`:4035-4038`);
- `closureValue = 'BACKUP_CLOSURE_COMPLETE'` when that worst is `COMPLETE` (`:4050-4051`);
- `groupResult('backupRecovery', …)` then reports group status `COMPLETE` (`:4076-4084` via `groupResult` `:636-656`).

**Fail-open trace (adversarial case A1).** Input: valid `asOf`; `BACKUP_DEPENDENCY` = `[one fully-MET postgresPitr entry]` (all required fields, `observed<=approved`, zero or positive targets). Expected under the plan: closure must not be complete — five fixed dependencies (`cosVersions`, `kmsKeyVersions`, `secretVersions`, `reconciliationOutbox`, `ivrLocalFacts`) were never accounted for. Actual: the one metric is `COMPLETE`/`MET`, `copiesOnly=false`, `closureStatus=COMPLETE`, `backupRecovery.closure.value = "BACKUP_CLOSURE_COMPLETE"`, group `backupRecovery` status `COMPLETE`, `partial:false`. Root stays `INCOMPLETE` only because the other eight groups are empty — the **recovery-closure group itself claims completeness** on 1/6 of the mandated closure.

**The code path is fixture-proven for single-entry arrays.** `s2-01-backup-recovery.json` cases `observed-exceeds-target` (`:217-246`, single entry → closure `NOT_MET`) and `asof-required` (`:249-277`, single entry → closure `DEPENDENCY_CLOSURE_INCOMPLETE`) demonstrate that a one-entry array is accepted and drives the closure verdict. The only `BACKUP_CLOSURE_COMPLETE` fixtures (`s2-01:7-96` complete-zero-rpo-rto, `s2-03:21-122` arithmetic-complete-but-not-production-ready) both include **all six** entries — so the validator (108/108 green) cannot detect the subset fail-open. This is a coverage hole aligned with the code hole, not a coincidence.

**Duplicate IDs compound it.** Nothing rejects duplicate `dependencyId` values (`computeBackupDependency` validates membership at `:3864` but uniqueness is never checked). Six entries that are all `dependencyId:"postgresPitr"` + `included:false` would yield `COPIES_ONLY` (fail-closed by accident), but six entries that are all `postgresPitr` + fully MET yield `BACKUP_CLOSURE_COMPLETE` with the other five dependency classes never mentioned. Additionally, `validate.mjs` `flattenMetrics`/`checkMetric` (`:77-86`, `:153-159`) only ever inspect `candidates[0]` for a given metric id, so a duplicate-id fixture would silently check only the first occurrence — the validator is structurally blind to this class too.

**Precedent.** The same file already enforces a required fixed set for nodes: `REQUIRED_ROLES` (`calc.mjs:130`) with `ROLE_NOT_DECLARED` / group `INCOMPLETE` when a required role is absent (plan §4.2 / `docs/18:321`). The backup group has no analogue despite being the more safety-critical closure.

**Required fix (bounded).** In `computeBackupRecovery`: (a) treat each of the six fixed IDs absent from the array as a missing closure member (emit a `DEPENDENCY_MISSING`-class metric or missing marker per absent ID, or at minimum force `closureStatus=INCOMPLETE` and never `BACKUP_CLOSURE_COMPLETE` unless all six IDs are present exactly once); (b) reject duplicate `dependencyId` values with `INVALID` + stable code; (c) add fixtures: subset-complete (1/6 MET ⇒ closure not `BACKUP_CLOSURE_COMPLETE`), duplicate-ID, and all-six-absent-but-array-nonempty.

### F-S2-R1-02 (P2) — fixed `CAPABILITY[]` set is not enforced; omitted capabilities are invisible

**Requirement.** Plan §4.1 (`:118`): "`CAPABILITY[]`: 固定 `CV-1`..`CV-5` + `regionAzSku`". `docs/18:434`: "`capabilityEvidence` 组包含固定 `CAPABILITY[]`：`CV-1`、`CV-2`、`CV-3`、`CV-4`、`CV-5`、`regionAzSku`".

**Implementation.** `computeCapabilityEvidence` (`calc.mjs:4190-4239`) maps whatever entries exist (`:4210-4212`) and only synthesizes a `capabilityEvidence.summary` metric when the array is **empty** (`:4213-4229`). A one-entry array (e.g., only `CV-1` `UNVERIFIED`, or only `regionAzSku`) produces a group of `NOT_VERIFIED` with no marker that `CV-2..CV-5` / `regionAzSku` (or `CV-1`) were never listed. `s2-02` case `claimed-complete-is-still-not-verified` (`:37-66`, only `CV-1`) and `s2-03` case `region-sku-unverified-gate` (`:124-145`, only `regionAzSku`) **codify** the subset behavior, so implementation and fixtures agree with each other but diverge from the plan's "固定" checklist semantics.

**Why this is P2, not P1.** The fail-closed direction is preserved: omitted capabilities cannot produce `COMPLETE`/verified — `UNVERIFIED`/`CLAIMED_VERIFIED` both cap at `NOT_VERIFIED` (`:4157-4162`), root constants stay `false`, and `DEPLOYMENT_EVIDENCE_INCOMPLETE`/`NO_CLOUD_VERIFICATION`/`A3_RISK_NOT_CLOSED` are always attached (`CAPABILITY_LIMITATIONS` `:167-171`, applied at `:4168-4171`/`:4226`/`:4238`). The defect is **checklist completeness** (an omitted capability is indistinguishable from a listed-UNVERIFIED one), not a false positive verdict. Contrast with F-S2-R1-01, where the subset can produce a false **positive** `BACKUP_CLOSURE_COMPLETE`.

**Required fix (bounded).** Either (a) emit per-absent-fixed-ID `INCOMPLETE` metrics (mirroring the backup fix) and update the two subset fixtures to include the remaining IDs or assert the new missing markers, or (b) obtain an explicit controller decision that `CAPABILITY[]` is an open subset of a closed vocabulary and amend the plan/`docs/18` wording accordingly. Do not leave plan and code contradicting each other silently.

### F-S2-R1-03 (P3) — `docs/README.md` diff is a full-file CRLF rewrite, not a one-line addition

**Claim.** Implementation report §2 (`a4-…-s2-implementation.md:40`): "`docs/README.md` 仅新增计划指定的单行入口". Plan §3 (`:56`): S2 README change is one row.

**Reality (reviewer-run).** `git diff HEAD -- docs/README.md` renders as a single hunk `@@ -1,150 +1,151 @@` that deletes every one of the 150 HEAD lines and re-adds them. `file docs/README.md` → "with CRLF line terminators"; `git show HEAD:docs/README.md | file -` → no CRLF (LF). The working copy converted the entire file LF→CRLF and appended the one A4 row. Content-wise the row is exactly the plan line at `docs/README.md:50`, and the validator's exact-line + link-uniqueness check (`validate.mjs:516-540`) still passes — but the byte-level change touches every line of a shared navigation file and the report's "only one line" statement is inaccurate.

**Required fix (bounded).** Restore LF line endings so the delta is the single added row, or amend the report to disclose the EOL conversion. Prefer restoring LF to keep the S2 diff auditable.

### F-S2-R1-04 (P3) — controller base hash and implementation-report base hash are both invalid objects

Task prompt and implementation report §0/§2 cite base `04c63de0e3243100370325f36e13de8a0791c2f4`. `git rev-parse HEAD` / `git log -1 --format=%H` → `04c63de87996d14d0d3701b399edf46e2a921ed9`. The 7-char prefix `04c63de` matches, so this is almost certainly a transcription error in the report/task, not a wrong commit — but a cited base that `git` cannot resolve breaks auditability of "diff against accepted S1". **Required fix:** correct the report's base hash to the real HEAD hash (documentation-only edit).

### F-S2-R1-05 (P3) — dependency metrics are all labeled `F-BACKUP-01` even when the verdict comes from `F-BACKUP-02`

Plan §4.3 (`:145` area / formula table): "每个 metric 必须带 formulaId". `F-BACKUP-02` is the observed-vs-approved comparison (`plan:295`, `docs/18:408`). Every per-dependency metric is built with `formulaId: 'F-BACKUP-01'` (`calc.mjs:3970`, also `:3848` for the non-object fallback), including metrics whose status/value are `NOT_MET`/`"NOT_MET"` produced by the `observedRpo/Rto > approvedRpo/Rto` branch (`:3938-3941`). `F-BACKUP-02` appears only in the group's `formulaIds` (`:4078`). A consumer tracing "which formula produced this verdict" is misdirected for the comparison metrics. **Required fix (bounded):** set the per-dependency `formulaId` to `F-BACKUP-02` when the verdict is MET/NOT_MET from the observation comparison (or carry both IDs), keeping `F-BACKUP-01` for the typed-completeness verdicts. Informational; does not affect status/value fail-closedness.

## 3. Verified-correct checklist (priority areas, with evidence)

| Priority area | Verdict | Evidence |
| --- | --- | --- |
| Six dependency closure | **Fail-open** — F-S2-R1-01 | `calc.mjs:4012-4054`; no presence check against `:138-145` |
| Zero RPO/RTO legal | Pass | `NonNegativeMeasure` allows 0 (`calc.mjs:326-329`, no `positive` spec on `:3868-3887`); `s2-01` `complete-zero-rpo-rto` (`:7-96`) expects `MET` for 0<=0 |
| Target vs observed | Pass | `observedRpo>Rpo || observedRto>Rto` ⇒ `NOT_MET`+`TARGET_NOT_MET` (`:3938-3941`); `<=` ⇒ `COMPLETE`/`MET` (`:3942-3944`); fixture `observed-exceeds-target` (`s2-01:217-246`) |
| `asOf`/date semantics, no wall clock | Pass | `compute` uses only `Date.parse` (deterministic); reviewer Grep for `Date.now\|Math.random\|new Date\|performance.now` over `calc.mjs` → **0 matches**; `readAsOf` (`:3757-3765`) never defaults; `applyFutureDateCheck` gated on `asOf.value` present (`:3833-3838`) so absent asOf skips future comparison; fixtures `asof-required` (`s2-01:249-277`, `ASOF_REQUIRED`+`ASOF_ABSENT_NO_FUTURE_CHECK`), `restore-date-in-future` (`:280-308`), `bad-asof-date` (`:401-428`), `no-asof-no-future-comparison` (`s2-02:145-171`) |
| Excluded vs missing | Pass | `included=false` ⇒ `INCOMPLETE`+`DEPENDENCY_EXCLUDED` with required-field missing paths filtered to only `dependencyId`/`included` (`:3912-3916`, `:3926-3928`); `included=true` + missing fields ⇒ `DEPENDENCY_MISSING` (`:3929-3931`); fixture `excluded-distinct-from-missing` (`s2-01:141-186`) asserts both discriminators and `missingInclude: []` on the excluded entry |
| Copies-only | Pass (for the all-excluded/all-empty cases it covers) | empty array ⇒ `backupRecovery.dependencies` `INCOMPLETE` (`:4015-4029`); all excluded ⇒ `copiesOnly` ⇒ value `COPIES_ONLY` + `COPIES_ONLY_INCOMPLETE` (`:4044-4046`); fixture `copies-only-incomplete` (`s2-01:189-214`); INVALID metrics correctly suppress the `COPIES_ONLY` value (status branch `:4042-4043` precedes `copiesOnly`) |
| Capability caller assertions & evidence completeness | Pass for claim fail-closed; **gap** for fixed-set completeness (F-S2-R1-02) | `UNVERIFIED` ⇒ `NOT_VERIFIED`/`"UNVERIFIED"` (`:4157-4159`); `CLAIMED_VERIFIED` complete ⇒ still `NOT_VERIFIED`/`"CLAIMED_VERIFIED"` (`:4160-4162`); missing any of `claimedBy/owner/source/evidenceRef/verifiedAt` ⇒ `INVALID`+`EVIDENCE_REQUIRED`+root error (`:4152-4156`); unknown status ⇒ `INVALID`+`TYPE_INVALID` (`:4114-4117`); fixtures `all-unverified`, `claimed-complete-is-still-not-verified`, `claimed-missing-evidence`, `invalid-capability-status` (`s2-02:6-34`, `:37-66`, `:68-92`, `:173-191`) |
| Root `cloudVerified`/`a3RiskClosed` constants | Pass | hardcoded `false` in the return literal (`calc.mjs:4303-4304`); `CALLER_CONTROL_KEYS` (`:84-90`) checked at `:4245-4247` → `CALLER_CONTROL_FORBIDDEN` without touching constants; fixture `caller-cannot-rewrite-root` (`s2-02:123-142`) pins all five forbidden keys and constant `false` values; `checkExpected` additionally re-asserts the four flags on every fixture case (`validate.mjs:147-152`) |
| Deployment/procurement gating | Pass | root `productionReady`/`procurementReady` hardcoded `false` (`:4301-4302`); `DEPLOYMENT_EVIDENCE_INCOMPLETE` on every capability metric/group (`:167-171`, `:4168-4171`, `:4226`, `:4238`); `forbiddenOutputStrings: ["FIT_EVIDENCE_COMPLETE"]` on all four `s2-03` cases (`s2-03:17`, `:121`, `:144` — substring check also excludes `LINE_FIT_EVIDENCE_COMPLETE` since it contains the token); no region/SKU/price/SLO/RPO/RTO value appears in any template leaf (all `value:null`, `provenance:MISSING` — template `:885-890` plus S1 groups) or in fixture expectations |
| S1 regression boundary | Pass | `git diff HEAD -- deployment/capacity/calc.mjs` deleted lines are only: the S1-only header comment, `const GROUP_IDS = […]` (split into `S1_GROUP_ID_LIST`/`S2_GROUP_ID_LIST`, `:16-26`), the `groupResult` signature (gained default `extraLimitations = []`, backward compatible, `:636`), the "seven-group" comment, a trailing `spool` array comma, root `asOf: null` (now `asOf.value`, which is `null` for S1 inputs without `asOf` via `readAsOf`), and the `S1_GROUP_IDS` export retarget (now the 7-item list, correct). **Zero S1 formula-body deletions.** `validate.mjs` stage s1 allows 7- or 9-group templates (`:217-225`) and checks only the 7 S1 groups' statuses (`:278-281`); stage s2 runs s1+s2 fixture files (`:618-619`) — matching controller's 86-case S1 regression inside the 108-case s2 run |
| Non-finite probes | Pass (static) | `readLeaf` rejects non-finite numbers with `NUMBER_NONFINITE` + root-invalidating `addError` (`calc.mjs:376-377`); validator injects real `NaN`/`±Infinity` per numeric leaf and asserts `error.path === leafPath` and root `INVALID` (`validate.mjs:405-451`); S2 injection covers the four new `seconds` leaves (`:379-391`, `:415-419`); controller's 471 = 459 + 12 corroborated by static leaf arithmetic (§1) |
| Determinism | Pass (static) | no wall-clock/randomness (Grep above); `checkDeterminism` double-computes the template and compares JSON (`validate.mjs:509-514`); per-case `repeat:true` double-run (`:485-490`); controller reports 3 repeat checks and 1 determinism check per stage |
| README uniqueness | Pass (content); EOL caveat F-S2-R1-03 | exact plan line once at `docs/README.md:50`; `checkReadmeLink` asserts exactCount==1 AND linkCount==1 (`validate.mjs:528-539`); reviewer Grep confirms a single hit |
| No invented cloud/region/SKU/price/SLO/RPO/RTO | Pass | template leaves all null/MISSING (validator asserts `value===null`, `provenance===MISSING`, `approval===MISSING` on every leaf, `validate.mjs:243-251`); fixtures use only `SYNTHETIC_TEST_ONLY` numbers; `forbiddenOutputStrings` and root-constant assertions cover the positive-claim directions; `docs/18` §7/§10 explicitly re-cite docs/12 decisions without choosing values (`docs/18:327-343`, `:460-462`) |
| No S2 scope drift / no A0–A3 contract change | Pass | `git diff HEAD --name-only` within the S2 slice touches only the allowed paths; the S2-allowed `docs/18` deletions are exclusively the S1-scope header lines (title/status/template-count/§1.4/“S1 明确未覆盖” section) replaced by S1+S2 scope text — no formula rows, no A0–A3 IDs, no OpenAPI/schema/registry files in the slice; implementation report §2/§10 declarations match |

## 4. Adversarial cases (traced or fixture-backed)

| # | Case | Expected under plan | Observed / predicted | Disposition |
| --- | --- | --- | --- | --- |
| A1 | One fully-MET `postgresPitr`, five fixed IDs absent, valid `asOf` | closure cannot be complete; five IDs unaccounted | `closure.value="BACKUP_CLOSURE_COMPLETE"`, group `COMPLETE` (trace of `:4012-4054`) | **F-S2-R1-01 fail-open** |
| A2 | Six entries, all `dependencyId:"postgresPitr"`, all MET | duplicate rejected; other five unaccounted | no uniqueness check (`:3863-3866` membership-only); closure `COMPLETE` | **F-S2-R1-01** |
| A3 | `BACKUP_DEPENDENCY` non-empty but every entry excluded, IDs all `postgresPitr` | not a valid six-excluded closure | `COPIES_ONLY` (fail-closed by accident — no false COMPLETE) | accepted, covered by A1/A2 fix |
| A4 | Only `CV-1` `CLAIMED_VERIFIED` complete; `CV-2..5`/`regionAzSku` absent | fixed-set gap visible | group `NOT_VERIFIED`, no missing markers (`s2-02:37-66` codifies) | **F-S2-R1-02** |
| A5 | `included=false` + malformed `approvedRpoSeconds: -1` | validate-before-short-circuit ⇒ INVALID | `invalidPaths` precede the `excluded` branch (`:3901-3925`) ⇒ `INVALID` | correct (fail-closed) |
| A6 | `included=false` + missing restore/RPO/RTO/owner/evidence | EXCLUDED, no missing-field noise | filter keeps only `dependencyId`/`included` in `missingPaths` (`:3912-3916`); fixture `s2-01:141-186` | correct |
| A7 | Full observations, no `asOf` | `ASOF_REQUIRED`, no future check, no comparison | branch order `missing → asOfMissing → observation → compare` (`:3920-3945`); fixture `s2-01:249-277` | correct |
| A8 | `observedRpo=0 <= approvedRpo=0` (zero targets) | legal `MET` | `NonNegativeMeasure` accepts 0; `:3942-3944`; fixture `s2-01:7-96` | correct |
| A9 | `restoreTestedAt` after `asOf` | `DATE_IN_FUTURE` INVALID | `applyFutureDateCheck` `:3833-3838` + `invalidate`; fixture `s2-01:280-308` | correct |
| A10 | `included: "yes"` (string) | `TYPE_INVALID` | `readPlainBoolean` `:3813-3817`; fixture `s2-01:311-338` | correct |
| A11 | Top-level `productionReady/procurementReady/approved/cloudVerified/a3RiskClosed: true` | `CALLER_CONTROL_FORBIDDEN` ×5, constants stay false | `:4245-4247` + hardcoded literals `:4301-4304`; fixture `s2-02:123-142` | correct |
| A12 | `CLAIMED_VERIFIED` with future `verifiedAt` and valid `asOf` | `DATE_IN_FUTURE` INVALID | future check runs before the claim branch (`:4123-4124` then `:4146-4148`); fixture `s2-02:94-120` | correct |
| A13 | `status:"VERIFIED"` (unknown) | `TYPE_INVALID`, never treated as verified | `:4114-4117`; fixture `s2-02:173-191` | correct |
| A14 | `NaN`/`+Infinity`/`-Infinity` injected into `approvedRpoSeconds` (programmatic) | `NUMBER_NONFINITE`, root `INVALID` | `readLeaf` `:376-377`; validator `:429-447`; controller 471-probe run green | correct (external runtime evidence) |
| A15 | `asOf: "not-a-date"` with a complete dependency | root + dependency `DATE_INVALID` INVALID | `readAsOf` invalidates and nulls (`:3759-3763`); dep adds `asOf` to invalidPaths (`:3899-3909`); fixture `s2-01:401-428` | correct |
| A16 | Six fully-MET deps + one claimed capability (arithmetic-complete gate case) | root still not production/procurement/cloud/A3-ready | fixture `s2-03:21-122` pins all four constants false and forbids `FIT_EVIDENCE_COMPLETE` | correct |
| A17 | Duplicate A4 README link line (hypothetical) | validator fails | `exactCount!==1` or `linkCount!==1` ⇒ issue (`validate.mjs:533-538`); currently 1/1 | correct |
| A18 | Malformed `approvedRpo` beside an excluded entry (validate-before-short-circuit for excluded) | INVALID, not silently EXCLUDED | `collectInvalid` over all leaf recs regardless of `excluded` (`:3901-3910`) | correct |

## 5. Conclusion

**Fail.**

The S2 slice is, on every other priority axis, a faithful and disciplined implementation of plan fix 2 / `docs/18` §7–§10: zero-RPO legality, target-vs-observed comparison, deterministic `asOf` date semantics without wall clock, excluded-vs-missing separation, copies-only aggregation, capability-claim fail-closure, root constant immutability against caller control, deployment/procurement gating, S1-regression preservation (zero S1 formula-body deletions), programmatic non-finite probes, template determinism, README uniqueness, and no invented cloud/region/SKU/price/SLO/RPO/RTO values. The controller-supplied runtime evidence (both syntax checks exit 0; s1 86/86 with 459 probes; s2 108/108 with 471 probes, 3 repeats, readmeLink=true) is internally consistent with independent static corroboration of every count.

However, **F-S2-R1-01 is a fail-open on the slice's headline fail-closed semantic.** The plan, `docs/18` §9.1 and the S2 task all define the recovery gate as a **six-dependency closure** over a fixed ID set; the implementation aggregates only caller-supplied entries, so a caller can obtain `backupRecovery.closure.value = "BACKUP_CLOSURE_COMPLETE"` and group status `COMPLETE` while five of the six mandated dependency classes were never presented. Duplicates of a single ID can do the same. The existing validator cannot detect this because no fixture exercises the subset-complete or duplicate-ID inputs — the green 108/108 result is therefore not evidence against this defect. Until the closure set is enforced (and fixture-covered), the S2 checkpoint must not be accepted.

### Required fixes (bounded, in order)

1. **F-S2-R1-01 (P1, blocking):** enforce the fixed six-ID closure in `computeBackupRecovery` — absent IDs must block `BACKUP_CLOSURE_COMPLETE` (per-absent-ID `DEPENDENCY_MISSING`-class markers or an equivalent forced-`INCOMPLETE` rule), duplicate `dependencyId` must be `INVALID`, and `s2-01` (or a new `s2-01` case) must add subset-complete, duplicate-ID and nonempty-but-all-IDs-absent behavioral assertions. Consider aligning `checkExpected`/`flattenMetrics` to fail on duplicate metric ids so the validator cannot be blinded.
2. **F-S2-R1-02 (P2):** either enforce the fixed `CV-1..CV-5`+`regionAzSku` set with absent-ID markers (and update `s2-02`/`s2-03` subset fixtures), or obtain an explicit controller ruling that the capability array is an open subset and amend plan/`docs/18` wording. Plan and code must not contradict each other.
3. **F-S2-R1-03 (P3):** restore LF line endings in `docs/README.md` so the S2 delta is truly the single plan row (or disclose the EOL conversion in the report).
4. **F-S2-R1-04 (P3):** correct the base hash in the S2 implementation report to `04c63de87996d14d0d3701b399edf46e2a921ed9`.
5. **F-S2-R1-05 (P3, optional bundled fix):** label per-dependency metrics with `F-BACKUP-02` (or both formula IDs) when the MET/NOT_MET verdict comes from the observed-vs-target comparison.

After fixes 1–2 (3–5 may be bundled), re-run `node --check` on both files plus `--stage s1` and `--stage s2` in a node-permitted environment and return for re-review. Do not enter later gates on the current tree.

### Operational declaration

This review wrote exactly one file: `docs/reviews/a4-s2-implementation-review-mimo26flash-20261026.md`. No implementation, fixture, specification, README, control document, prototype or screenshot was edited. No commit, push, PR, cloud operation or later gate was performed.
