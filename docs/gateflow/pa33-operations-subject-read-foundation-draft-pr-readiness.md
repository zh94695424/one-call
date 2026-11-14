# PA33 Operations subject read foundation — draft PR readiness

## Status

- Work unit: `PA33-operations-subject-read-foundation`
- Gate: `ready-to-open-draft-PR`
- Branch: `codex/pa33-next-work-unit`
- Base: merged PA32 `699a18a6f218aaef3ab8c9ab164c1b4101c6fadf`
- Accepted slice: `5cec8a6da16c3531b8e21afadee85144320641a3`
- Accepted aggregate review: `5a84503ac342befdc4d8e56af587f5a273ef5357`
- Remote status: user authorized push; Draft PR [#44](https://github.com/zh94695424/one-call/pull/44) has been created. PR review is the current gate.

## Delivered scope

- exactly `GET /api/v1/operations/subjects`
- exactly `GET /api/v1/operations/subjects/{subjectId}`
- independent opaque employee session, exact audience, current ACTIVE `ADMIN|OPS`
- six structured filters, stable `(createdAt desc, subjectId desc)` seek pagination
- opaque AES-GCM filter-bound cursor with key rotation support
- exact safe list/detail projections with no full number, media, playback credential or hidden storage key
- owner-local SQL and one read-only `REPEATABLE_READ` snapshot with asserted/restored owner roles
- no writes, audit facts, Outbox events, trial/consent/grant/balance mutation or business-serialization locks
- request-local cooperative five-second budget in addition to per-statement database limits
- no OpenAPI, docs/14/15/17, schema, grant or migration change; V001-V017 remain frozen

Final branch diff from merged PA32 contains 54 paths: the approved 37 implementation paths plus 17 PA33 gate/review artifacts. Every local commit from that base is an intended PA33 gate/bookkeeping commit, and no generated `target`, XML, JAR, class, `node_modules`, `.codex` or private `.pa*` path is tracked.

## Verification

- Full clean PostgreSQL16 validation for the accepted slice: Surefire `644/644`, Failsafe `476/476`, total `1120`, zero failures/errors/skips, BUILD SUCCESS in 27:49.
- Exact source=CI=POM=XML: `70/644 + 29/476 = 99/1120`; 106 new PA33 identities, token 1121, POM Failsafe includes 29.
- Fresh aggregate-fix runs: focused Surefire 83/83; container-profile 644/644 Surefire + 36/36 targeted Failsafe, zero failures/errors/skips.
- PR-review fix 1: focused 45/45; container-profile 644/644 Surefire + 36/36 targeted Failsafe, zero failures/errors/skips. All 106 PA33 identities remain unchanged; detailed results and changed paths are in `pa33-pr-review-fix-1.md`. These are local runs, not hosted CI or a fresh full 476-container run.
- Controller independent service regression after the aggregate fix: 16/16.
- Packaged default/web JAR smokes and 116-probe production dependency firewall passed.
- A1 passed. Raw A2-A5 retains the expected CRLF parsing failure; the required LF-equivalent mirror passed A2/A3/A4/A5 including s2 doc links.
- All 22 frozen hashes match; exactly V001-V017 exist; no V018.
- Historical slice and aggregate reviews passed; subsequent PR-review test-evidence findings are addressed in `pa33-pr-review-fix-1.md`, pending fresh independent A/B PR re-review.

## Findings and residuals

- `PA33-IMPL-01`: fixed.
- `PA33-IMPL-02`: fixed.
- `PA33-PLAN-CORRECTION-01`: accepted and verified.
- `PA33-AGG-01`: cooperative request-budget fix accepted and re-reviewed.
- `PA33-AGG-02`: owner exception classification fixed and re-reviewed.
- `PA33-AGG-04`: authorized bounded role-map/import consolidation fixed; broader ownership relocation rejected/out of scope.
- `PA33-AGG-03`: deferred with owner. Frozen detail A0 metadata omits three tables actually read; correction belongs to a separately authorized Contract/OpenAPI + Security/Data work unit.

The deadline is cooperative at operation boundaries, not an asynchronous cancellation SLA. In-flight driver/owner/cleanup work may outlive five seconds, but no later owner operation or batch starts after expiry. The 100-batch/10,100-candidate limit is a fail-closed scan bound, not a guaranteed success target within five seconds; usable capacity depends on database RTT, query and role-handoff costs and the remaining budget. Pool/index/SLO, production six-role login, employee IdP/verifier, cursor key custody/rotation, hosted clean-Linux CI and production acceptance remain external owner risks. No production-readiness claim is made.

## Remote-action boundary

The branch is locally ready for a Draft PR titled:

`PA33: add Operations subject read foundation`

The user authorized push and Draft PR creation. Do not merge, approve, mark ready for review, request GitHub reviewers or write external comments.
