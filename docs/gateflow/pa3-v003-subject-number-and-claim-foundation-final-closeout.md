# PA3 V003 subject number and claim foundation final closeout

## Work unit

- ID: `PA3-v003-subject-number-and-claim-foundation`
- Type: architecture-sensitive migration and persistence feature work unit
- Draft PR: #11, https://github.com/zh94695424/one-call/pull/11
- Branch: `codex/pa3-v003-subject-number-and-claim-foundation`
- Base: merged PA2 `main` commit `58c13de895cccf44c0101a865b97e19ae2afef20`
- Accepted PR-review checkpoint: `77e9649f1d2c4dcabe4e3e955233e2631b117101`

## What changed

- Added frozen migration `V003__subject_number_and_claim.sql` with business subject, caller-number binding, append-only subject claim and current subject risk-state tables.
- Added the exact 36-column, 25-check, seven PK/ordinary-unique, 18-index, seven ordinary immediate RESTRICT-FK, four trigger-function and four-trigger contract, with narrow owner/runtime grants and zero PUBLIC table/function/column privileges.
- Added fail-closed PostgreSQL 16 evidence for catalog and ACL exactness, subject owner set-once behavior, append-only claims, risk-state lifecycle, active binding uniqueness and the two-connection commit/rollback race.
- Kept V001 and V002 byte-for-byte unchanged and kept Flyway, PostgreSQL JDBC and Testcontainers out of the production runtime artifact.
- Added no V004+ object, application service, API, controller, JPA aggregate/repository, frontend, runtime DataSource/Flyway wiring, provider integration, cloud provisioning or production-acceptance claim.

## Verification

- Default Maven verification passed 10/10 tests.
- PostgreSQL `16.15` full container verification passed 25/25; two full post-fix runs were green, focused subject-owner and append-only-claim methods passed, and focused active-binding concurrency passed three independent runs.
- Boot smoke, production fat-jar dependency firewall, exact source/CI/XML 25-method equality, catalog/ACL/hash and whitespace checks passed.
- V001 SHA-256 remains `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045`; V002 remains `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5`; V003 is `1D3FFB5FFD60D1BE5F85B93B87F52C727E7B43DAD5929280123CC567AB873FB1`.
- GitHub Actions run `40574911853` passed `java-runtime` and `contract-regressions` on exact accepted PR-review checkpoint `77e9649f1d2c4dcabe4e3e955233e2631b117101`.
- At closeout intake, local HEAD, `origin/codex/pa3-v003-subject-number-and-claim-foundation` and PR #11 head matched that checkpoint; the PR remained Open, Draft and `CLEAN`.

## Review and findings

- S1 finding F-S1-01 added an exact zero-PUBLIC-column-ACL assertion and reached final `已修复` status after independent re-review.
- S2 findings F-S2-01 and F-S2-02 corrected only the checkpoint hash and line-count evidence; both reached final `已修复` status. The executable timestamp defect found during controller validation was corrected with `GREATEST(observed_at, clock_timestamp())` and all full/focused reruns passed.
- Aggregate deepreview passed with no material finding.
- Independent PR review `docs/reviews/pr-11-review-20261028-032059.md` passed with zero material findings; SHA-256 `03EAB3EC6855DDFFA8C1E26288D57F5C93152C41ED55507A38CC8FEA23CF311F`.
- The first PR-review runner attempt was terminated after a recursive executable search and prolonged network wait produced no result, stderr or artifact. The single bounded MiMo retry completed successfully, used the exact GitHub CLI path and wrote only the review artifact.
- No accepted finding remains unresolved, and no blocking open question or unclassified risk remains.

## Documentation

- Binding implementation plan: `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-plan.md`.
- Execution controller: `docs/gateflow/project-execution-control.md`.
- Aggregate review: `docs/reviews/code-review-20261028-030006.md`.
- PR review: `docs/reviews/pr-11-review-20261028-032059.md`.
- Runtime-facing evidence summary: `backend/README.md` and `.github/workflows/ci.yml`.

## Remaining risks and owners

- PostgreSQL 16.15 deparse strings remain version-sensitive and the `postgres:16` image remains floating: Backend/QA owns fail-closed test maintenance; Platform/CI owns digest and supply-chain hardening.
- `RETIRED` terminality, claim/binding/risk service codes, advisory-lock serialization and Q2/Q17/Q23/Q24 remain with the later subject-number application service and the named Security/Platform/Database/product owners.
- Production migration identity, owner/runtime role provisioning and `SET LOCAL ROLE` capability remain Platform/Database work under Q17/Q23.
- The active-binding race test is a bounded two-connection probe, not stress/chaos coverage; broader concurrency validation remains Backend/QA work.
- Owner-role `TRUNCATE` is the accepted frozen design boundary; runtime roles have no TRUNCATE. Any policy change requires a separately confirmed design work unit.
- Windows LF/CRLF handling remains optional Platform/tooling hardening.
- GitHub Actions reported non-blocking platform warnings for Node 20 compatibility forcing, `actions/setup-java@v4` deprecation and the future `ubuntu-latest` runner migration; Platform/CI owns upgrading and revalidating the workflow in a separate tooling work unit.
- This work unit proves build, migration and synthetic database behavior only. Production cloud, carrier/IVR, load, restore, security and manual acceptance remain in later architecture phases and the acceptance matrix.

## Issue status

This feature work unit has no associated GitHub issue. No issue link, closing keyword, closeout comment or issue mutation is required or claimed.

## Closeout state and next entry point

`PA3-v003-subject-number-and-claim-foundation` reached `final closeout pass`. Draft PR #11 remains unmerged by design; no approval, ready-for-review action, reviewer request, external comment, merge, branch deletion or production-acceptance claim occurred.

After the user merges PR #11, update local `main` from `origin/main`, create a new isolated branch/worktree from that merged base, and start a fresh Phaseflow goal confirmation for the tentative next bounded Phase A work unit `PA4-v004-catalog-order-and-payment-foundation`. Its candidate boundary is only `V004__catalog_order_and_payment.sql` and executable evidence for the frozen catalog, order and payment persistence contract in `docs/14` sections 4.16-4.22, 5.3, 9, 10 and 11.4. V005+, application workflows, provider integration and production provisioning require separate confirmed scope.
