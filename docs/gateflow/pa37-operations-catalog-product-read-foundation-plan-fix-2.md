# PA37 Operations Catalog Product Read Foundation — Plan Fix 2

Task label: `pa37-operations-catalog-product-read-plan-fix2-gpt-20261117-01`
Date: 2026-11-17 (Asia/Shanghai)
Scope: only the two accepted Controller fix1-intake findings; plan text only.
Status: bounded author correction complete; no acceptance or later gate authorized.

## 1. Provenance and exact artifacts

- Authoritative pre-fix1 cumulative plan: SHA-256
  `e0a5d3435cecd2e60919021757326bf6bab4c36d68a787eb3ef8f14b3ee8e069`
  (35,714 bytes / 514 lines, as retained by Control and both independent reviews).
  Fix1's statement that this hash could not be recovered was incorrect. This is the
  correction for `PA37-PLAN-FIX1-REV-02`; historical fix1 is not rewritten.
- Input cumulative plan after fix1 / before fix2: SHA-256
  `8758c2b6ba7f0d79640786c3e0e5d28ef414722336f4eb3cfdc8d8980c51f800`
  (18,348 bytes / 251 lines).
- Output cumulative plan:
  `docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md`
  SHA-256 `7d9cb67c07485655783b90ed74857df336b3c8a91e4613daa2dc626ae69098c6`
  (19,346 bytes / 270 lines; canonical UTF-8/LF).
- Unchanged historical artifact:
  `docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-1.md`
  SHA-256 `2f539bcd7e22fac208a2bafe51f29c27499df57c79c34725ae6c08ce1da1ecb4`
  (5,529 bytes / 68 lines).
- This new fix2 artifact has no before hash. Its byte/line counts and SHA-256 are
  reported after writing in the completion report, not embedded recursively here.

Authority checked: `docs/gateflow/project-execution-control.md` PA37 checkpoint,
`docs/reviews/plan-review-20261117-180736.md` (A), and
`docs/reviews/plan-review-20261117-182054.md` (B), plus fix1 and current
OpenAPI/source/POM/CI facts. Control and both reviews retain the same pre-fix1 hash.

## 2. Accepted fix1-intake finding corrections

### PA37-PLAN-FIX1-REV-01 — Exact six-class unit inventory

Removed the redundant proposed ACL-only unit class and its eight method identities,
including its focused Maven selector. No remaining reference to that class exists
in either fix2 output. The six originally intended unit/architecture classes retain
all their exact names and method names, and the two IT classes retain theirs.

Cumulative-plan §5 assigns every unique ACL assertion to already-listed methods:
approved tables/columns and structural projection to boundary and projection methods;
`FOR UPDATE`, DDL, and advisory-lock scans to `rejectsWritesAndLocks`; unchanged V004
matrix to `observesFrozenRoleMatrix`; truthful read-only/trigger mutation-denial
attribution to the existing three DML-denial methods; lock/DDL denial, success/failure
restoration, and unchanged grants/roles to their existing IT methods. These are
assertions within existing methods, not additional test identities or files.

Directly counted new-file inventory:

| Test-bearing class | Suite | Exact method count |
|---|---|---:|
| `OperationsCatalogProductReadRequestTests` | unit | 8 |
| `OperationsCatalogProductCursorCodecTests` | unit | 8 |
| `OperationsCatalogProductReadServiceTests` | unit | 18 |
| `OperationsCatalogProductReadWebTests` | unit | 10 |
| `OperationsCatalogProductReadConfigurationTests` | unit | 8 |
| `OperationsCatalogProductReadBoundaryTests` | unit/architecture | 8 |
| `OperationsCatalogProductReadsIT` | container | 20 |
| `OperationsCatalogProductReadRoleLeastPrivilegeIT` | container | 16 |

Zero-test helpers: `OperationsCatalogProductReadFixture.java` and
`OperationsCatalogProductReadTestProviders.java`; neither enters a CI identity map.
Exactly ten new test Java files = six unit + two IT + two zero-test helpers.
The complete exact method-name lists remain in cumulative-plan §5; each listed
count equals its distinct method-name count, and command selectors equal those sets.

| Inventory | Current | Delta | Final |
|---|---:|---:|---:|
| Unit classes / methods | 83 / 788 | 6 / 60 | 89 / 848 |
| Container classes / methods | 34 / 556 | 2 / 36 | 36 / 592 |
| Total classes / methods | 117 / 1,344 | 8 / 96 | 125 / 1,440 |
| Annotation-line scan | 1,345 | 96 | 1,441 |
| Scoped container-smoke Failsafe includes | 34 | 2 | 36 |
| Global POM include elements | 36 | 2 | 38 |
| Existing guarded production manifest | 291 | 8 | 299 |

Unit additions are `8+8+18+10+8+8=60`; container additions are `20+16=36`.
Exactly two new Failsafe includes remain:
`**/OperationsCatalogProductReadsIT.java` and
`**/OperationsCatalogProductReadRoleLeastPrivilegeIT.java`; no Surefire include delta.
The global count is all POM include elements, not an additional Failsafe suite.
The existing production guard separately pins V018 and counts 291 other files;
the raw current production tree has 292 files. Its existing LF-normalized manifest
digest was independently reproduced as
`95c4eef4059fcc92d2408a493bbd89420a7fa5d5a5a3e5cdbd4dc164cc1623e0`.
No new exclusion or production/test path change is authorized by this fix.

CI uses exactly the six `pa37_unit_additions` entries and two
`pa37_container_additions` entries listed in §5, with length vectors
`[8,8,18,10,8,8]` and `[20,16]`. Recomputed PA37 historical subtraction is
`[848-60,592-36] == [788,556]` and `1441-96 == 1345`, so historical assertions remain
`pa37_start_totals == [788, 556]` and `pa37_start_line_scan_total == 1345`.
All earlier historical subtraction forms must also subtract these same PA37 method
sets. Current literals remain `method_totals == [848, 592]`, sum 1440,
`line_scan_total == 1441`, class lengths 89/36, scoped include length 36, and exact
Surefire/Failsafe XML totals 848/592. No helper or removed identity enters any map,
source-subtraction set, selector, or literal. This fix changes no CI/POM/source file.

### PA37-PLAN-FIX1-REV-02 — Historical hash correction

The authoritative pre-fix1 hash is recorded in §1 with its Control/A/B provenance.
The fix1 claim of unavailability is superseded by this correction, not silently
edited away. The pre-fix1 hash and the different fix1-output/fix2-input hash must not
be conflated. Historical fix1 bytes and its SHA-256 remain unchanged.

## 3. Nine prior closures retained without reopening scope

| Finding | Preserved cumulative-plan closure |
|---|---|
| `PA37-PLAN-REV-01` | §5 existing `EvidenceRoleIvrAssociationAclBoundaryTests` tree/CI lockstep, exact 291→299 guarded manifest/digest maintenance, universal coverage and no new exclusions. |
| `PA37-PLAN-REV-02` | §5 exact eight production paths, ten test files, six unit/two IT method dictionaries, two excluded helpers, arithmetic, POM includes, current/historical CI literals. |
| `PA37-PLAN-REV-03` | §§1/3 no `catalog_offer` read/join/EXISTS; inventory annotation is not a join mandate; offer-less DRAFT and multi-channel membership assertions remain. |
| `PA37-PLAN-REV-04` | §§2–4 two-phase root-bounded paging in one snapshot, lookahead roots, versions for returned root IDs, zero-version retention, no duplicate/skipped roots. |
| `PA37-PLAN-REV-05` | §3 truthful unchanged V004 SELECT/INSERT/selected UPDATE/EXECUTE/USAGE matrix, approved SQL projection, read-only/trigger denial attribution and broad-role owner residual. |
| `PA37-PLAN-REV-06` | §3 `COLLATE "C"` for root order and exclusive anchor, matching Java UTF-8 byte ordering and reachable fixtures. |
| `PA37-PLAN-REV-07` | §2 authority before parsing/ring resolution, authority×ring precedence, blank cursor/detail/error/429 rules and no rate limiter. |
| `PA37-PLAN-REV-08` | §6 repository `check:s3` gate and focused Maven no-tests guards remain; only the redundant class selector is removed. |
| `PA37-PLAN-REV-09` | §§2/4/6 declared null wire fields, QUOTE/CNY behavior, exact authority projection, ordering and cursor framing/512 limit remain. |

Reverse-applying only the bounded fix2 task-label/class-list/selector/assertion-block
delta reproduces the exact fix1 input bytes/hash. Thus every other cumulative-plan
line, including prior closure sections and residual owners, is unchanged.
One S1 end-to-end read family remains. Schema, OpenAPI, grants, roles, datasource,
pool, key ring and mutation boundaries remain frozen; no business scope is reopened.

## 4. Validation evidence and stop

Actual validation commands (from the repository root):

```text
python3 /tmp/pa37-fix2-validate.py
diff -u /tmp/pa37-fix2-before.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md
wc -c -l docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-2.md
sha256sum docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-2.md docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-1.md
```

The local validation script checks every listed distinct method count, six unit/two
IT/two helper inventory, both selector sets, exactly two include additions, actual
source baseline/annotation scan, XML-parsed POM scopes, guarded production count and
digest, CI literals/subtraction arithmetic, all nine closure rows, retained scope
prohibitions, no stale removed-class reference or unassigned identity or alternate
placement, correct historical hash, unchanged fix1 hash and exact reverse-delta hash.
Both output files pass strict UTF-8/LF, no BOM, U+FFFD, conflict marker or trailing
whitespace checks. `diff` exit 1 denotes the expected bounded text changes.
Initial local validation assertions were corrected to distinguish explicit
co-location prohibitions, global/scoped POM counts and the guard's existing separate
V018 pin; no repository implementation file was changed to satisfy the validator.

`git status --short` was attempted but failed because the managed worktree Git
metadata points to an unavailable Windows path. No Git status/diff cleanliness
claim is made. All repository write commands target only the cumulative plan and
this new artifact; validation scratch files are under `/tmp`.
No npm/Maven/runtime/container/hosted gate was run; their retained commands are for
implementation only. No source, test, POM, CI, migration, OpenAPI, Control, existing
review, historical fix1 or product document was edited. No staging, commit, push,
PR, approval, reviewer request, external comment, merge, close, implementation or
review-gate action was performed. Stop here for Controller intake.
