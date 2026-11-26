# PA42 customer order read and UNKNOWN visibility foundation — final closeout

## What changed

- Added the two frozen customer read operations:
  - `GET /api/v1/customer/orders`
  - `GET /api/v1/customer/orders/{orderId}`
- Added trusted account/workspace ownership filtering, stable `(createdAt DESC, orderId DESC)` keyset pagination, opaque authenticated cursors, indistinguishable detail 404 behavior, immutable stored-snapshot projection and computed issuance status.
- Preserved `paymentStatus=UNKNOWN` visibility without any reconciliation, provider callback, repair or business write.
- Added V020 as the sole new migration: exact customer schema `USAGE` plus column `SELECT` ACL, owner grantors, no write/DDL/default-ACL expansion.
- Added read-only transaction/role lifecycle checks and zero-write database evidence.
- Added exact OpenAPI/HTTP, PostgreSQL 16, ACL, transaction, cursor, recovery, snapshot, issuance, packaged-artifact and firewall coverage.

Product/bookkeeping scope is 85 paths: 16 new and 69 modified. The implementation report and review/Control artifacts are additional gate evidence outside that product manifest.

## What was verified

- final 85-path manifest: 85 lines / 13,160 bytes / SHA256 `623e04f24d0fdedde5b87140caf7603e51f0d1949b184d5f90d89993b713bfba`
- exact architecture/module/PA25 guards: 138/138
- clean `-Pcontainer-smoke verify`: Surefire 1,164/1,164; Failsafe 764/764; zero failures/errors/skips; BUILD SUCCESS
- PostgreSQL 16.16
- source/CI/POM/fresh-XML identity parity: 168 classes / 1,928 unique identities; 1,929 annotation hits; 46 POM includes
- packaged content: 811/811 compiled entries byte-equal; 23/23 source resources; 20 migrations with V020 exactly once
- HTTP smoke: 212/212
- dependency/JAR firewall: 116/116
- frozen bytes: 33/33; V001–V019, OpenAPI inputs and docs12/14/15/17 unchanged
- `git diff --check`: pass

The final clean full run completed naturally after approximately 85 minutes. Whole-JAR reproducibility is not claimed; the final build was 40,192,842 bytes / SHA256 `20a3d3e57ca290180ab86181ec78e5bc0ce57cf279ae7b5e832f9fe463890b1e`, with content parity verified against target classes/resources.

## Reviews and findings

- Initial code review A/B were independently submitted before Controller intake; accepted findings were fixed and re-reviewed.
- Final code-review re-review A/B both passed with zero material findings after all accepted findings were marked `已修复`.
- Aggregate deepreview found the missing row↔snapshot/cursor negative anchors and missing historical value-domain fail-closed checks. The first B aggregate conclusion was quarantined because it disclosed an isolation breach.
- Aggregate fix added existing-method test anchors, cursor mutation/limit-change coverage, row↔snapshot mismatch coverage, and snapshot-reader validation for frozen `entitlementType`/`productCode` domains. The authorized stale manifest pins were updated without changing lists, counts or assertion strength.
- Fresh repaired-aggregate review A/B passed. A reported no material finding; B reported two Low residuals only.
- Final PR review A/B passed with no blocker. PR-review findings were adjudicated:
  - missing server-side diagnostics on fail-closed paths: `deferred-with-owner` to Backend/Customer Orders observability;
  - defensive row-hash recheck: `rejected-with-reason` because the reader already validates the caller hash against the canonical stored payload.

## Docs updates

- Accepted plan and reconciliation history are in `docs/gateflow/pa42-customer-order-read-and-unknown-recovery-foundation-plan.md`.
- Implementation and all fix/re-review evidence are in `docs/gateflow/pa42-customer-order-read-and-unknown-recovery-foundation-implementation.md`.
- All plan, code, aggregate and PR review artifacts are retained under `docs/reviews/`.
- `backend/README.md`, `docs/operations/pa25-production-database-identity-provisioning-runbook.md`, PA25 runtime identities, migration guards and CI/POM inventories were synchronized only for the exact V020/current-boundary delta.

## Residual risks and owners

- Cursor key sharing between catalog-offer and order families: Security/Operations, separate credential-policy work unit.
- PERSONAL leading scan and issuance fan-out: Customer Data/Database, docs18 capacity/index work unit.
- Separate PA41 create/readback projection value-domain parity: Customer Orders/Data Integrity later work unit.
- Valid `limit=100` end-to-end HTTP test anchor: Test Maintenance.
- Server-side diagnostics for sanitized fail-closed paths: Backend/Customer Orders observability.
- Corrupt/out-of-band historical facts remain fail-closed; no repair/backfill is performed.
- Organization visibility remains current-account plus selected-workspace visibility, not organization-wide sharing.
- Production deployment acceptance is NOT EXECUTED.
- Stale historical PA25 method-name and V020 generic rejection-category precision remain deferred with Test Maintenance and Database/Operations respectively.

## PR and external state

- Draft PR #54: `https://github.com/zh94695424/one-call/pull/54`
- base: `main`
- head branch: `codex/pa42-customer-order-read-and-unknown-recovery-foundation`
- PR remained OPEN and Draft at the last verified metadata read; `mergeStateStatus` was CLEAN.
- Hosted Actions runs appeared initially, but the latest status rollup was empty. Hosted CI is therefore **unavailable/not claimed green**; exhaustive local execution evidence is authoritative for this closeout.
- No merge, approval, mark-ready transition, reviewer request, external comment, issue mutation or branch deletion occurred.

## Final gate state

`draft-PR-pass` and `final closeout pass` are accepted. The work unit is complete locally.

After the user merges PR #54, fetch the updated `main`, start from that merged commit in a fresh managed worktree/branch, and begin a new Phaseflow goal confirmation for the next user-approved ONECALL work unit. Do not reuse PA42 goal confirmation or start from the unmerged branch.
