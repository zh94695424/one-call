# PA39 Operations Catalog Draft Write Foundation — Bounded Plan Fix 2

Task: `pa39-operations-catalog-draft-write-foundation-plan-fix2-retry1-gpt-20261120-01`.
Date: 2026-11-20. Gate: plan correction only; no implementation or runtime acceptance.
Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa39-next-work-unit/codex-call`.
Branch: `codex/pa39-next-work-unit`. Supplied HEAD: `a142796ae7e2561c5fba10365887f11f77576589`; supplied tree: `e6e205d657173cc5cba2739ec16a752f02a798e3`.

## 1. Exact Finding Closure

The partial plan-fix2 update is completed here. The implementation allowlist now explicitly includes the two inherited README-pin test files:

- `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java`

Each file is modified only in its existing frozen-input guard. Retain the exact ordered 26-row table, all 25 non-README raw/LF pairs, all existing test identities and counts, and the historical PA35 comparison. The active README row in each guard uses raw SHA256 `ca9d8aa4a92066f6e9a3f44fb9ad5c66f156a569fcc5ab478d05410f987627b5`, LF SHA256 `0816cc1c1ef622eaec9d4d7545e1ee8c6dc0f35813d4748b41d44dc9d4cec20b`, and independently recomputed aggregate digest `afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761`. The 26-row aggregate remains the SHA256 of sorted UTF-8 path, NUL, raw-hash bytes, LF-hash bytes tuples with no separator or terminal LF; the concatenation is 3507 bytes. The historical README pair and historical aggregate remain unchanged in the historical expectation.


The prior findings are closed at plan level: README edits remain current-section-only before the PA36 marker; five CI inventory assertions are explicitly authorized; lock SQLSTATE 55P02/55P03 remains 409 without retry; strict absent replace members remain 422 before state checks; recursive UTF-16 canonicalization and exact integer vectors remain pinned; recognized corruption remains 503 while valid legacy facts remain 409; ownership remains one command/repository slice; and no query-derived policy or dead limiter is introduced.


## 2. Final Allowlist And Arithmetic


Final implementation allowlist: **19 new + 10 modified = 29 paths**.


- 8 new and 3 modified production Java paths.
- 10 new and 4 modified test Java paths; the four modified test files include the two inherited IVR guards above.
- 1 modified POM, 1 modified CI workflow, 1 modified README, and 1 mandatory new implementation report.
- 18 new Java files, 9 new executable classes, and 120 new methods; the four modified tests add no identity.
- Final counts: 104/1018 Surefire, 40/666 Failsafe, 144/1684 executable suites/methods, 1685 annotation lines, 40 POM includes, 318 governed manifest entries, 21 resources/18 migrations, and 14 modules/13 edges/5 catalog dependencies.

The implementation report `docs/gateflow/pa39-operations-catalog-draft-write-foundation-implementation.md` is absent at this plan gate. Its absence is a blocked implementation-path finding, not a green result or scope waiver; only the later authorized implementation may create it.


## 3. Validation Boundaries


§9.1 remains external-projection-only: all Maven, PostgreSQL, XML, JAR, contract, and frozen checks use one recorded validation projection, never the primary worktree. `npm ci` is forbidden in the primary worktree; if later unavoidable, it may run only in the external projection with locked dependencies and must not be confused with a completed validation gate. The plan does not run it here.


Required focused selectors remain exact: 7 new unit classes/84 methods; 3 architecture classes/25 methods; two one-method guards (1/1 each); 6 frozen-regression classes/52 methods; 13 PA37/PA38 unit classes/138 methods; 2 draft live classes/36 methods; 4 PA37/PA38 live classes/74 methods; and the repeated race/fault selector is exactly 2 classes/8 methods in each of 3 independent processes.


The clean full sweep must prove 104/1018 Surefire XML and 40/666 Failsafe XML, 144/1684 distinct identities, source=CI=POM=XML, and zero skips/errors/failures. JAR smoke/firewall checks remain complete and unchanged; contract validation includes OpenAPI plus the full protocol/security/deployment/acceptance blocks, with frozen OpenAPI/docs12/docs14/docs15/docs17 bytes and exactly V001–V018. No implementation report, full XML, JAR, or contract result is claimed here.


The deferred >2^53 implementation-test obligation remains mandatory: exact integer reconstruction, adjacent-value distinction, canonical bytes/hashes, benefit quantity `9007199254740993`, and Long.MAX_VALUE. This fix neither waives it nor adds an identity.


## 4. Scope And Custody


No V019, OpenAPI, docs12/docs14/docs15/docs17, Control, source implementation, migration, grant, public contract, unrelated test, dependency-install artifact, commit, stage, push, review action, or implementation entry is authorized. No dependency installation or runtime command was run. The only permitted repository changes are the cumulative plan and this fix2 artifact.


Exact next gate: **fresh peer-isolated A/B plan re-review**. No implementation, approval, or later gate is entered.
