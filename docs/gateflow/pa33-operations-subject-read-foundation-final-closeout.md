# PA33 Operations subject read foundation — final closeout

## Delivery

PA33 delivered exactly two authenticated Operations read routes:

- `GET /api/v1/operations/subjects`
- `GET /api/v1/operations/subjects/{subjectId}`

The implementation provides independent opaque employee-session verification with exact audience and current ACTIVE `ADMIN|OPS` authority; six structured filters; stable descending keyset pagination; opaque AES-GCM filter-bound cursors; safe list/detail projections; owner-local SQL in one read-only `REPEATABLE_READ` snapshot; asserted/restored owner roles; and zero domain writes, audit facts, Outbox events or entitlement/trial/consent mutations. A request-local monotonic five-second fail-closed budget supplements per-statement database limits.

Final reviewed product/test head: `ced4d5362f81c0187efe6b71a2f8d10b79bf2727`.

Draft PR: https://github.com/zh94695424/one-call/pull/44

## Verification

- Accepted-slice full PostgreSQL16 clean: Surefire 644/644, Failsafe 476/476, total 1120, zero failures/errors/skips.
- Final PR test-evidence fix: focused 45/45; container-profile 644/644 Surefire + 36/36 targeted Failsafe, zero failures/errors/skips.
- Exact source=CI=POM=XML: `70/644 + 29/476 = 99/1120`, 106 PA33 identities, token 1121, POM includes 29.
- Hosted `contract-regressions`: PASS; A1-A5 contract steps passed.
- Hosted `java-runtime`: PASS in 27m08s, including build/container smoke, inventory, default/web JAR smokes and production dependency firewall.
- All 22 frozen hashes match; exactly V001-V017 exist; no V018, migration, grant or OpenAPI change.
- Plan review, slice review, aggregate review and PR review all used independent A/B reviewers with peer-free frozen inputs. Required fixes underwent fresh re-review and passed.

## Findings

- `PA33-PLAN-REV-01..06`: fixed and independently re-reviewed.
- `PA33-IMPL-01`, `PA33-IMPL-02`: fixed.
- `PA33-PLAN-CORRECTION-01`: accepted and validated.
- `PA33-AGG-01`, `PA33-AGG-02`, authorized bounded `PA33-AGG-04`: fixed and re-reviewed.
- `PA33-AGG-03`: deferred-with-owner to Contract/OpenAPI + Security/Data.
- `PA33-PR-REV-01..04`: fixed and independently re-reviewed.
- Two Low PR re-review residuals—positional test-shape labels and source-regex schema confinement—are deferred-with-owner to Backend test hardening. Neither is a current silent-pass or production defect.

No accepted finding remains unfixed. No blocking open question remains for the confirmed PA33 goal.

## Residuals and owners

| Residual | Owner / destination |
| --- | --- |
| Cooperative deadline can be exceeded by an already-running owner/driver/cleanup operation; no later owner/batch starts after expiry | Backend/Platform/Database deployment timeout and SLO validation |
| Role-handoff round trips and RTT-dependent 100-batch/10,100-candidate fail-closed scan bound | Backend/Platform/Database performance/SLO and non-PII observability work |
| Missing stable-sort covering index and production count-volume/latency evidence | Database/Platform/Backend EXPLAIN/load/SLO work; schema change needs new authorization |
| Capacity ceilings return sanitized 503 rather than partial data | Product/Backend/Database accepted bounded-capacity design |
| Production employee verifier/IdP, six-role login provisioning, cursor key custody/distribution/rotation | Identity/Platform/Security/Database Q17/Q23 deployment readiness |
| Snapshot authority can precede concurrent disable; no cross-page export snapshot | Backend/Security accepted read-snapshot semantics |
| Frozen detail A0 metadata omits `entitlement_grant`, `evidence_record` and `customer_order` | Contract/OpenAPI + Security/Data separate authorized work unit |
| Scanner-shape labels and runtime relation allow-list test hardening | Backend test hardening; no current runtime defect |
| Hosted CI action-version deprecation warnings | Platform/CI maintenance; current final-head checks passed |

Production readiness, live IdP/key provisioning, cloud/runtime acceptance and arbitrary-cardinality success are not claimed.

## Issue and remote state

PA33 is not tied to a GitHub issue, so no issue link, closing keyword or issue closeout comment is required.

PR #44 remains OPEN and Draft. No merge, approval, mark-ready action, reviewer request or external comment was performed. The branch is `codex/pa33-next-work-unit`, based on merged PA32 main `699a18a6f218aaef3ab8c9ab164c1b4101c6fadf`.

## Completion and next entry point

PA33 is complete at `final closeout pass`. Its controller-only closeout commit contains this summary and the final Control update.

Next entry point: after the user merges PR #44, fetch/update `main` from that merge commit and start a fresh Phaseflow goal confirmation for the next work unit. No next work-unit scope is authorized by PA33; do not start it from this unmerged branch.
