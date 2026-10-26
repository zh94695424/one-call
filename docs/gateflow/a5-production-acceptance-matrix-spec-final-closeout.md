# A5 Production Acceptance Matrix Specification Final Closeout

- Work unit: `A5-production-acceptance-matrix-spec`
- Type: architecture-sensitive production acceptance specification deliverable
- Gate: `final closeout`
- Status: `final closeout pass`
- Date: 2026-10-26
- Draft PR: `https://github.com/zh94695424/one-call/pull/7`
- Accepted PR review checkpoint: `b18d9956c91df0779efc325df201c1f8969f5416`
- Reviewed-content and bookkeeping push verified at: `0d1ab9963943c2876dd0eb09d16dfe392c5898d0`

## What changed

- Delivered the A5 production acceptance matrix for the accepted A0-A4 contracts: 16 critical coverage units, six mandatory axes, 96 cells, 90 represented scenarios, six contract-backed `NOT_APPLICABLE` cells and zero deferred cells.
- Added a dependency-free validator and 127 focused fixture cases for source integrity, state/effect semantics, terminal absorption, duplicate/out-of-order/denied behavior, evidence admission, replay/immutability and authority boundaries.
- Defined evidence artifact, manifest and acceptance-record admission while keeping the static authority registry empty. Static/model/synthetic success cannot become runtime `PASS` or `productionAccepted=true`.
- Added the PR-review count-only guard: caller root fields `specificationCoverageComplete` and `specCoverageComplete` fail with exactly `A5_COUNT_ONLY_COVERAGE`, without a duplicate `A5_SECTION_EXTRA` issue.
- Replaced the hardcoded determinism claim with a two-probe CLI gate. Only matching successful child status/stdout/stderr with one probe marker can publish `deterministic=true`; mismatch or unusable probe results fail closed with `A5_DETERMINISM_FAILURE`.
- Updated the binding plan, canonical specification, implementation/fix reports, review evidence and Draft PR description. Existing HTML prototypes remain demonstration/QA assets and are not production acceptance evidence.

## Verification

- `node --check acceptance/validate.mjs`: pass.
- A1 OpenAPI `npm ci` and `npm run check:s3`: pass with 49 operations and zero document/A0 drift. Local npm reported the existing Node `>=22` engine requirement while the controller shell used Node 20.20.1; this successful run is not a supported-runtime policy decision.
- A2 IVR protocol stage S3: pass with 25/25 fixtures.
- A3 security stage S3: pass with 23/23 fixtures.
- A4 capacity/deployment stage S2: pass with 113/113 cases and all readiness flags false.
- A5 S1 twice: exit 0, empty stderr and byte-identical stdout, SHA-256 `085752aebfe43c33837fac5f13cf840f7e75d5466b5270c33e4cca8063751280`.
- A5 S2 twice plus `--check-doc-links`: exit 0, empty stderr and byte-identical stdout, SHA-256 `810029be650c4b6e06694ecf26389eff7e6d90e237dd50bd3285c6e26536dba9`.
- Direct S1/S2 probe mode: exit 0, exactly one `deterministic=probe`, zero `deterministic=true`; normal parent output contains exactly one final marker and no probe marker.
- All five A5 JSON files parse. S1-01 contains 27 cases; both new count-only cases use the exact sole expected code `A5_COUNT_ONLY_COVERAGE`; complete S1 fixture validation passes.
- `docs/README.md` contains exactly one docs/19 link; scoped whitespace checks pass.
- Final remote branch and `refs/pull/7/head` both resolve to `0d1ab9963943c2876dd0eb09d16dfe392c5898d0`; local versus upstream is `0 0`.
- GitHub reports PR #7 Draft/Open/Clean with `statusCheckRollup=[]`. Absence of repository checks is not represented as passing CI.

These checks prove static specification completeness, deterministic validator behavior and synthetic fixture behavior only. They do not prove real carrier lines, IVR media, database/queue/cache behavior, payment, COS/KMS, cloud capability, load, restore, penetration, legal/manual review or production acceptance.

## Finding status

- Plan review findings `F-A5-PR-01..06` are fixed; accepted open questions were closed or explicitly owned before the plan checkpoint.
- S1 findings `F-01..F-04`, `F-R1-01..04`, `F-R2-01` and their bounded fixes are independently re-reviewed and closed.
- S2 findings `F-S2-01..07` are `已修复` and independently re-reviewed.
- Aggregate finding `F-AGG-01` is `已修复`; the stale plan SHA was corrected without changing runtime behavior.
- PR findings `F-PR7-01` and `F-PR7-02` are `已修复`; fresh MiMo Flash re-review found no new material issue.
- No accepted finding, blocking open question or unclassified residual risk remains.

## Residual risks and destinations

| Residual | Owner | Destination |
| --- | --- | --- |
| Real integration, line, cloud, restore, load, security and manual evidence is not executed; `runtimeEvidence=NOT_RUN` and `productionAccepted=false` remain binding | Telephony / IVR / Backend / QA / Infrastructure / Security / Product / Operations | Authorized production acceptance execution and release-governance gates |
| Static `authorityRegistry=[]` intentionally makes final production PASS unreachable | Product / Security / Operations | Define and approve production acceptance authority during release governance; do not add synthetic authority |
| Immutable evidence store, retention/WORM, artifact storage and cross-ledger identity require runtime implementation and proof | Infrastructure / Security / Backend / Evidence | Evidence platform implementation, retention/legal decision and integration tests |
| PR #7 has no configured CI; A1 declares Node `>=22`, while one controller run used Node 20.20.1; the two-probe gate doubles validator cold-start cost | Platform / CI | Pin supported Node 22 LTS and run the A1-A5 regression suite in repository CI before production merge policy is enforced |
| Probe comparison currently uses UTF-8 decoded strings; current Node validator output is valid UTF-8 and canonical ASCII summaries retain exact byte hashes | Platform / CI | If the output protocol later admits arbitrary binary/non-UTF-8 bytes, compare raw Buffers and add failure-path fixtures |
| The immutable S1 implementation report records the historical 25-case fixture digest, while the accepted PR fix has 27 S1-01 cases | Controller / audit history | Retain the historical report unchanged; this closeout and PR fix/re-review artifacts record the accepted 27-case state |
| Accepted A0-A4 runtime, policy, provider, capacity and cloud residuals remain open | Their existing named owners | Carry into Phase A-D implementation and the corresponding A5 runtime evidence rows; this static matrix does not close them |

Every remaining risk has an owner and destination. None is an unclassified blocker for closing this static A5 work unit.

## Documentation and external state

- Canonical specification: `docs/19-production-acceptance-matrix.md`.
- Machine model and fixtures: `acceptance/v1/acceptance-matrix-v1.json` and `acceptance/fixtures/`.
- Validator: `acceptance/validate.mjs`.
- Execution truth: `docs/gateflow/project-execution-control.md`.
- Initial PR review: `docs/reviews/pr-7-review-20261026-203107.md`.
- Passing PR re-review: `docs/reviews/pr-7-review-20261026-210525.md`.
- The Draft PR body was corrected from 125 to 127 fixture cases and now records both closed PR findings.
- The first ordinary HTTPS push timed out. A command-scoped GitHub edge resolution plus `http.sslBackend=schannel` completed the non-force push. No global Git configuration, hosts entry or remote URL was changed.
- This work unit is not backed by a GitHub issue. No issue link, closing keyword, closeout comment or issue mutation is required or claimed.
- No merge, approval, mark-ready action, reviewer request, external PR comment or branch deletion was performed.

## Next entry point

After the user merges Draft PR #7, update local `main` from `origin/main`. Then start a fresh Phaseflow goal confirmation for the first bounded Phase A platform-foundation implementation work unit, tentatively `PA0-production-runtime-bootstrap`, using architecture section 15 Phase A and accepted A0-A5 contracts as binding inputs.

The goal-confirmation pass must bound the first implementation increment before planning. The intended starting boundary is production repository/runtime scaffolding (Java 21, Spring Boot/Spring Modulith and Maven), module dependency enforcement, supported Node/toolchain pinning and the minimum repeatable CI/Testcontainers smoke path. It must not silently absorb the 44-table Flyway implementation, all identity/activation domains, cloud provisioning, IVR integration or later Phase B-D features into one oversized work unit.

Do not start Phase A from this unmerged A5 branch, and do not reuse A5 authorization to invent patch versions, production credentials, cloud capabilities, concurrency, retention, SLO/RPO/RTO or acceptance authority.
