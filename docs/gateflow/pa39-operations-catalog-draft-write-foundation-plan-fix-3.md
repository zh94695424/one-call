# PA39 Operations Catalog Draft Write Foundation — Bounded Plan Fix 3

Task: `pa39-operations-catalog-draft-write-foundation-plan-fix3-gpt-20261120-01`.
Date: 2026-11-20. Gate: documentation-only wording/executability correction; no implementation or runtime acceptance.
Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa39-next-work-unit/codex-call`.

## 1. Inputs And Scope

Binding input: the PA39 Goal Confirmation and accepted round-2 dispositions in `docs/gateflow/project-execution-control.md`. Read the cumulative plan, fix2 and both round-2 artifacts:

- `docs/reviews/plan-review-20261120-195850.md` (A).
- `docs/reviews/plan-review-20261120-202116.md` (B).
- Input cumulative plan: 115,699 bytes / SHA256 `8aef7d3a47fca00d9ea0e5cd87baf1321a68146d9794f93f8560eb0ff04b7e1b`.
- Unchanged fix2: 5,273 bytes / SHA256 `fe130c618b2c8cbed8bf0dca1645e2cf4424d0b6b8cbfe2c868a83bb4bebce81`.

Only the cumulative plan is updated and this fix3 artifact is added. Existing blocked implementation is read-only evidence, not an edit target. Fix2 and the review/Control artifacts remain unchanged; fix3 supersedes contradictory operative wording without rewriting historical evidence.

## 2. Finding Closure

| Finding | Bounded documentation correction | Status |
| --- | --- | --- |
| `PA39-PLAN-RR2-B-F03` (Medium) | §§9/9.1/9.2/9.4, §11.2 and the environment/prohibition language consistently allow and require lockfile-only `npm ci` only inside the external validation projection during later authorized validation. §9.4 places it before `npm run check:s3` and the unchanged extracted contract blocks. | Closed at plan level; no install or contract execution claimed |
| `PA39-PLAN-RR2-A-01` (Low), B F-01/F-02 | §§8/9.1 replace absolute IVR immutability with exactly two exceptions: `OperationsIvrSessionReadBoundaryTests` and `OperationsIvrSessionReadsIT` may update only their README raw/LF FROZEN rows and aggregate digest under §7.4, with all test identities and other pins unchanged. No edit may bypass normalization. | Closed at plan level; no test edited |
| B F-04 | §1.1/§10 identify earlier Control custody hashes as historical/superseded rather than current pins, preserving every value and requiring dispatched custody to be re-recorded. | Closed; Control unchanged |
| B F-05 | Repair space-loss phrases in prose, including exact/active 26-row, all 1564, their 48, all 18, current inventory arithmetic and corrected 238/339-byte wording. | Closed; values and executable identities unchanged |
| B F-06 | §10 calls `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9` the PA25 raw hash guard, not the LF hash guard. | Closed; raw/LF values and frozen semantics unchanged |

The only dependency materialization exception is lockfile-only `npm ci` in `$PA39_VALIDATION_ROOT/api/openapi`, within the recorded external validation projection. It is allowed and required there before later contract validation, not in this documentation-only gate. The primary/source worktree must remain without installs or `node_modules`. Package and lockfile hashes must match before/after; no dependency upgrade, non-lockfile install, version drift or lockfile rewrite is authorized. The implementation report must record Node/npm versions, command/exit status and the projection-local `node_modules` manifest in external evidence. Failure remains an environment block, never permission to weaken validation or install in the source worktree.

## 3. Preserved Invariant Inventory

| Invariant | Unchanged requirement |
| --- | --- |
| Surface and slice | Exactly POST `/api/v1/operations/catalog/versions` and PUT `/api/v1/operations/catalog/versions/{versionId}/draft`; one S1 |
| Implementation allowlist | 19 new + 10 modified = 29 paths: 8 new/3 modified production Java, 10 new/4 modified test Java, modified POM/CI/README, one new implementation report |
| Java files | 298 production / 190 test |
| Surefire | 104 classes / 1,018 methods |
| Failsafe | 40 classes / 666 methods |
| Executable totals | 144 classes / 1,684 methods; 1,685 annotation lines |
| POM and manifest | 40 Failsafe includes; 318 governed production manifest entries |
| Test identity digest | `6041250383865a75ff49037c1f736bb87db552561206b9990aedaf8604a51770` |
| Other inventory | 21 resources / 18 migrations; 14 modules / 13 edges / 5 catalog dependencies |
| Frozen inputs | Exactly V001–V018, no V019; OpenAPI and docs12/14/15/17 unchanged |
| IVR pins | Exact ordered 26-row FROZEN set, all 25 non-README raw/LF pairs and historical PA35 expectations unchanged |
| Authorized active README pins | Raw `ca9d8aa4a92066f6e9a3f44fb9ad5c66f156a569fcc5ab478d05410f987627b5`; LF `0816cc1c1ef622eaec9d4d7545e1ee8c6dc0f35813d4748b41d44dc9d4cec20b` |
| Authorized active aggregate | `afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761`; unchanged sorted-path/NUL/raw32/LF32 algorithm, 3,507-byte input |
| Existing implementation | All current blocked source/test/POM/CI/README and other implementation bytes untouched |

The mandatory future report remains `docs/gateflow/pa39-operations-catalog-draft-write-foundation-implementation.md`; it remains absent and is not created by this fix. The exact focused selectors, clean source=CI=POM=XML oracle, JAR/firewall checks, full OpenAPI/protocol/security/deployment/acceptance chain and zero-skip/error/failure expectations are retained. No future runtime result is asserted.

The >2^53 implementation-test obligation remains mandatory: exact integer reconstruction, adjacent-value distinction, canonical bytes/hashes, benefit quantity `9007199254740993`, and Long.MAX_VALUE, within the existing identities. No technical behavior, source path, count, identity or canonical rule changes.

## 4. Verification And Next Gate

Verification is documentation/static only: compare pre/post SHA256 manifests of all existing worktree files; permit only the cumulative plan to differ and this fix3 artifact to be added; compare all existing 64-hex plan values, the frozen pin table, implementation-path inventory and fenced commands (the sole command addition is future projection-only `npm ci`). Check stale prohibitions and prose-spacing examples, and confirm no primary `node_modules`. Artifact SHA256/byte sizes are reported separately at completion to avoid a self-referential artifact hash.

Measured results: **PASS**. All 2,406 existing worktree files were SHA256-compared: only the cumulative plan differs; the other 2,405 files retain their exact bytes, only this fix3 artifact is added, and no file is deleted. All original 64-hex plan values remain in order, the §10 frozen table is byte-identical, and §§2–7 numeric values and inline identities/paths/vectors are unchanged. Every fenced command/vector is unchanged except the one future projection-only `npm ci` line. The stale prohibitions and reported spacing examples are absent. The primary worktree has no `node_modules`, and the future implementation report remains absent. These checks do not claim runtime acceptance.

No source, test, POM, CI, README, OpenAPI, migration, Control, review or private helper is changed. Nothing is staged, committed, pushed, published, merged, approved or submitted for review. No dependency installation, implementation, build or runtime validation is performed.

Documentation-fix blockers: **none**. Previously recorded implementation/runtime blockers, including the missing implementation report and uncompleted acceptance gates, remain pending; this fix only makes the validation instructions executable.

Exact next gate: **fresh peer-isolated A/B plan re-review**. Stop here; no implementation entry or reviewer request is authorized.
