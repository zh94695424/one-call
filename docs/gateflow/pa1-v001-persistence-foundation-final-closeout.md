# PA1 V001 persistence foundation final closeout

## Work unit

- ID: `PA1-v001-persistence-foundation`
- Type: architecture-sensitive persistence-foundation feature work unit
- Draft PR: #9, https://github.com/zh94695424/one-call/pull/9
- Branch: `codex/pa1-v001-persistence-foundation`
- Base: merged PA0 `main` commit `9fe3619af968d896400742d90df819bb66d868bd`
- Accepted PR-review checkpoint: `f48d6fddef5ad0b6826bde6c168dc68fdb212cb7`

## What changed

- Added the frozen V001 PostgreSQL migration with the six S1 schemas, 15 NOLOGIN roles, platform idempotency, transactional Outbox and append-only audit foundation.
- Added fail-closed PostgreSQL 16 Testcontainers coverage for migration application and failure visibility, exact inventory, idempotency concurrency and finalize-once behavior, Outbox replay/conflict/rollback, audit immutability, function metadata, grants and hostile caller search paths.
- Kept Flyway, PostgreSQL JDBC and Testcontainers test-scoped; the production fat jar still excludes Flyway, JDBC pool, PostgreSQL, jOOQ, Spring Security and Testcontainers dependencies.
- Updated CI and backend evidence for the exact 12-method container suite without adding V002+, runtime DataSource/Flyway wiring, domain APIs, UI, workers or cloud resources.

## Verification

- Default Maven verification passed 10/10 tests.
- The complete PostgreSQL 16 container profile passed 12/12 tests, with additional focused idempotency-concurrency repetitions passing 1/1.
- Boot smoke, production fat-jar dependency firewall, dependency-scope verification and whitespace checks passed.
- V001 SHA-256 remains `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045`.
- GitHub Actions run `40458384939` passed `java-runtime` and `contract-regressions` on exact PR head `f48d6fddef5ad0b6826bde6c168dc68fdb212cb7`.
- At closeout intake, local HEAD, `origin/codex/pa1-v001-persistence-foundation` and PR #9 head all matched that exact commit; the PR remained Open, Draft and `CLEAN`.

## Review and findings

- S1 code review closed two low findings before its accepted checkpoint; S2, S3 and aggregate deepreview reported no material finding.
- Independent PR review artifact `docs/reviews/pr-9-review-20261027-133921.md` passed with zero material findings after two evidence-only corrections: three abbreviated SQL signatures were expanded to their exact V001 contracts, and the provider identifier was corrected to `mimo26flash`.
- The corrected PR review artifact SHA-256 is `6816308D44A24F9DAEEC8D0EA575BC2C8D24B1A684697723086501707E83B2EC`.
- The evidence corrections did not change source, migration, tests, CI or the review verdict. No accepted finding remains unresolved, and no blocking open question or unclassified risk remains.

## Documentation

- Binding implementation plan: `docs/gateflow/pa1-v001-persistence-foundation-plan.md`.
- Execution controller: `docs/gateflow/project-execution-control.md`.
- Aggregate review: `docs/reviews/code-review-20261027-131028.md`.
- PR review: `docs/reviews/pr-9-review-20261027-133921.md`.

## Remaining risks and owners

- Floating `postgres:16` test image: Platform/CI owns digest pinning in supply-chain hardening; no digest is guessed in PA1.
- Q17/Q23 production migration identity, role provisioning and `SECURITY DEFINER` creation capability: Platform/Database/Security owns the deployment/security decision before production database rollout.
- Only `idempotency_begin` has a live hostile-`search_path` probe; the other three functions are catalog-proven. Backend/QA owns later persistence-test hardening.
- The audit encrypted-envelope/denylist success matrix is intentionally bounded. Backend/QA owns later persistence-test hardening.
- Shared-container method isolation and PostgreSQL 16 catalog/deparse forms require fail-closed maintenance. Backend/QA owns future test review and major-version changes.
- `audit_owner` TRUNCATE capability is an accepted frozen V001 design boundary. Operations-audit/Database owns any later contract change.
- Windows LF/CRLF conversion remains optional repository hardening owned by Platform/CI.
- This work unit proves build, migration and synthetic database behavior only. Production cloud, carrier/IVR, load, restore, security and manual acceptance remain in their later architecture phases and acceptance matrix.

## Issue status

This feature work unit has no associated GitHub issue. No issue link, closing keyword, closeout comment or issue mutation is required or claimed.

## Closeout state and next entry point

`PA1-v001-persistence-foundation` reached `final closeout pass`. Draft PR #9 remains unmerged by design; no approval, ready-for-review action, reviewer request, external comment, merge, branch deletion or production-acceptance claim occurred.

After the user merges PR #9, update local `main` from `origin/main`, create the next isolated branch/worktree from that merged base, and start a fresh Phaseflow goal confirmation for the tentative next bounded Phase A work unit `PA2-v002-identity-organization-employee-foundation`. Its candidate boundary is only the V002 identity, organization membership and employee tables, guards, grants and executable PostgreSQL evidence from `docs/14` section 11.3; V003+, domain workflows and runtime production provisioning require separate confirmed scope.
