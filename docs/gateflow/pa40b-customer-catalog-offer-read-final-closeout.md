# PA40B customer catalog offer read — final closeout

- Work unit: `PA40B-customer-catalog-offer-read`; sole slice `PA40B-S1`.
- Status: **final closeout pass**.
- Draft PR: https://github.com/zh94695424/one-call/pull/52, base `main`, head branch `codex/pa40b-customer-catalog-offer-read`.
- Accepted PR-review checkpoint: `5d6dc75e5ede741dc710e262b15e811a0e572dfd`.

## What changed

- Added only `GET /api/v1/customer/catalog/offers`, using the existing customer pool, customer transaction wiring and `customer_iam_role`.
- Added the catalog-owned customer query port/reader, customer-owned orchestration, endpoint-isolated HMAC cursor, exact 13-field offer / 4-field benefit DTO and customer problem mapping.
- Enforced trusted account/workspace/channel/audience authority, ACTIVE version/offer lifecycle, optional offer-type narrowing, exact authorization columns, deterministic keyset ordering, lookahead pagination and zero business writes.
- Added PostgreSQL16, web, service, codec, configuration and architecture guarantees plus CI/POM/inventory and README updates. No migration, OpenAPI, docs12/14/15/17, PA25 SQL/runbook, pool, credential, role, membership or grant was added or changed.

## What was verified

- Final uninterrupted exact-candidate run: `mvn -B -ntp -o -Pcontainer-smoke verify` → `BUILD SUCCESS`, `EXIT=0`, Surefire 110 classes / 1071 methods + Failsafe 42 classes / 701 methods = 152 / 1772, all zero failures/errors/skips.
- PostgreSQL16 catalog read tests: `CustomerCatalogOffersIT` 19/19 and `CustomerCatalogOfferReadPrivilegesIT` 16/16.
- CI inventory ledger: `EXIT=0`, source=CI=POM=XML 110/1071 + 42/701 = 152/1772, scan 1773, POM includes 42, full historical ladder intact.
- Jar smoke and dependency firewall passed on the implementation candidate; A1/A3/A4 contract validators passed. Pre-existing A2/A5 input limitations and hosted CI availability remain explicit, not claimed as passes.
- Four code-review artifacts and two aggregate-deepreview artifacts passed with no material defect. Two accepted code-review Lows were fixed and independently re-reviewed.
- Final PR review A/B both returned `pass-with-risks`; all new observations were closed, rejected with reason or deferred with owner/destination.

## Documentation

- Plan, plan fixes, six plan reviews, implementation report, bounded code-review fix, four code reviews, two aggregate reviews and two PR reviews are committed.
- Implementation report §10 records the post-fix exact-candidate validation and supersedes its earlier sandbox-only limitation.

## Finding status

- No accepted product finding remains open.
- `PA40B-CR-B-02` and `PA40B-CR-B-04`: `已修复`.
- PR review exact-head suite/evidence gaps and stale-head bookkeeping: `已修复`.
- Review-bundle manifest purity: `rejected-with-reason`; the actual PR diff contains no build/evidence/helper/log path.
- TRACE shared-500, audience documentation precision, secret-runbook inventory, cursor TTL/rotation and future status rechecks: classified residual/deferred with named owners and destinations in Control.

## Remaining risks and owners

- Production TLS/pg_hba, cursor-key custody/rotation, pool drain and rollback: Security/Operations/Platform/Database.
- Hosted HTTP rate limiting/429 and long-lived cursor hardening: Security/Product/Backend.
- Non-CNY out-of-band ACTIVE catalog rows and capacity/index validation: Database/Product/Platform.
- TRACE normalization and audience wording: Backend/API boundary/documentation maintenance.
- Hosted CI infrastructure: Controller/Platform; local exact-candidate evidence is green, hosted green is not claimed.

## Issue status and next entry

- No GitHub issue is associated with PA40B, so closing-keyword, issue-comment and issue-closeout actions are not applicable.
- After the user merges PR #52, fetch updated `origin/main`, create a fresh managed worktree and `codex/` branch from the merge commit, preserve existing worktrees/private helpers, reread Control and design sources, and perform a fresh Phaseflow goal confirmation for the next bounded work unit. PA40B defines no next work unit.
- No merge, approval, mark-ready, reviewer request, external comment or issue mutation occurred.
